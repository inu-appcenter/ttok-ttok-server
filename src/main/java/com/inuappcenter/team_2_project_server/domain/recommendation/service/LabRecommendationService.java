package com.inuappcenter.team_2_project_server.domain.recommendation.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.recommendation.client.FactChatGatewayClient;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabCandidateContextRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabCandidateRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.enums.SearchSourceType;
import com.inuappcenter.team_2_project_server.domain.recommendation.repository.LabSearchDocumentRepository;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.RecommendationPromptBuilder.CandidateContext;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.RecommendationPromptBuilder.RecommendationPick;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 연구실 추천 (RAG)
 * 1. 질문을 임베딩한다.
 * 2. 검색: 질문 벡터와 가까운 연구실 후보를 찾는다. (프로필 + 논문 유사도, {@link LabSearchDocumentRepository#findCandidates})
 * 3. 선택: 후보들의 프로필과 관련 논문을 LLM에 주고, 질문에 맞는 연구실을 최대 3개 고르게 한다.
 * 4. LLM이 고른 id가 후보 안에 있는지 검증하고(없는 연구실을 지어내지 못하게), 연구실 정보는 DB에서 채운다.
 * <p>
 * 외부 API(임베딩, LLM) 호출 동안 DB 연결을 잡고 있지 않도록 메서드 전체를 트랜잭션으로 묶지 않는다.
 * DB 조회는 각 리포지토리 호출 단위의 읽기 트랜잭션으로 처리된다.
 */
@Slf4j
@Service
public class LabRecommendationService {

    // LLM에 넘길 후보 연구실 수. 평가셋에서 후보 20개 안에 핵심 정답이 들어온 비율이 99%였다
    static final int CANDIDATE_COUNT = 20;
    // 후보마다 프롬프트에 붙일 관련 논문 수
    static final int PAPERS_PER_CANDIDATE = 3;
    // 검색 점수 = 프로필 유사도 70% + 가까운 논문 3편 평균 유사도 30% (평가셋에서 가장 좋았던 조합)
    static final double PROFILE_WEIGHT = 0.7;
    static final int PAPER_TOP_N = 3;
    // 최종 추천 최대 개수
    static final int MAX_RECOMMENDATIONS = 3;

    private final LabSearchDocumentRepository labSearchDocumentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final FactChatGatewayClient factChatGatewayClient;
    private final RecommendationPromptBuilder promptBuilder;
    private final String embeddingModel;
    private final String chatModel;

    public LabRecommendationService(
            LabSearchDocumentRepository labSearchDocumentRepository,
            LaboratoryRepository laboratoryRepository,
            FactChatGatewayClient factChatGatewayClient,
            RecommendationPromptBuilder promptBuilder,
            @Value("${ai.recommendation.embedding-model:text-embedding-3-small}") String embeddingModel,
            @Value("${ai.recommendation.chat-model:claude-haiku-5-5}") String chatModel
    ) {
        this.labSearchDocumentRepository = labSearchDocumentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.factChatGatewayClient = factChatGatewayClient;
        this.promptBuilder = promptBuilder;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    // pgvector에 넘길 벡터 문자열: '[0.1,0.2,...]'
    static String toVectorLiteral(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }

    public LabRecommendationResponseDto recommend(String question) {
        String normalizedQuestion = question.strip();

        // 1. 질문 임베딩. 실패하면 검색 자체를 할 수 없으므로 오류 응답
        float[] questionVector = factChatGatewayClient.embed(embeddingModel, List.of(normalizedQuestion))
                .map(List::getFirst)
                .orElseThrow(() -> new MyException(ErrorCode.RECOMMENDATION_UNAVAILABLE));
        String queryVector = toVectorLiteral(questionVector);

        // 2. 검색: 후보 연구실
        List<Long> candidateIds = labSearchDocumentRepository
                .findCandidates(queryVector, PROFILE_WEIGHT, PAPER_TOP_N, CANDIDATE_COUNT).stream()
                .map(LabCandidateRow::getLaboratoryId)
                .toList();
        if (candidateIds.isEmpty()) {
            // 검색 문서가 아직 만들어지지 않은 상태 (인덱싱 전)
            log.error("연구실 추천 후보가 없음. 검색 문서 인덱싱이 되어 있는지 확인 필요");
            throw new MyException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }

        // 3. 선택: LLM이 후보 중 최대 3개를 고른다. 실패하면 검색 순위 상위 3개로 대체 (추천 이유 없음)
        Map<Long, CandidateContext> contexts = loadContexts(queryVector, candidateIds);
        String userMessage = promptBuilder.buildUserMessage(normalizedQuestion, candidateIds, contexts);
        List<RecommendationPick> picks = factChatGatewayClient.chat(chatModel, RecommendationPromptBuilder.SYSTEM_PROMPT, userMessage)
                .flatMap(promptBuilder::parse)
                .map(selected -> keepCandidatesOnly(selected, candidateIds))
                .orElse(null);

        boolean fallback = picks == null;
        if (fallback) {
            log.warn("LLM 연구실 선택 실패, 검색 순위로 대체");
            picks = candidateIds.stream()
                    .limit(MAX_RECOMMENDATIONS)
                    .map(laboratoryId -> new RecommendationPick(laboratoryId, null))
                    .toList();
        }

        // 4. 연구실 정보는 LLM 답이 아니라 DB에서 채운다
        Map<Long, Laboratory> laboratories = laboratoryRepository
                .findWithProfessorByIdIn(picks.stream().map(RecommendationPick::laboratoryId).toList()).stream()
                .collect(Collectors.toMap(Laboratory::getId, Function.identity()));
        List<LabRecommendationResponseDto.Item> items = picks.stream()
                .filter(pick -> laboratories.containsKey(pick.laboratoryId()))
                .map(pick -> LabRecommendationResponseDto.Item.of(laboratories.get(pick.laboratoryId()), pick.reason()))
                .toList();

        return new LabRecommendationResponseDto(!fallback && items.isEmpty(), items);
    }

    // 연구실마다 프로필 1개와 질문과 가까운 논문 몇 개
    private Map<Long, CandidateContext> loadContexts(String queryVector, List<Long> candidateIds) {
        Map<Long, String> profiles = new HashMap<>();
        Map<Long, List<String>> papers = new HashMap<>();
        for (LabCandidateContextRow row : labSearchDocumentRepository.findCandidateContexts(queryVector, candidateIds, PAPERS_PER_CANDIDATE)) {
            if (SearchSourceType.PROFILE.name().equals(row.getSourceType())) {
                profiles.put(row.getLaboratoryId(), row.getContent());
            } else {
                papers.computeIfAbsent(row.getLaboratoryId(), id -> new ArrayList<>()).add(row.getContent());
            }
        }

        Map<Long, CandidateContext> contexts = new HashMap<>();
        for (Long laboratoryId : candidateIds) {
            contexts.put(laboratoryId, new CandidateContext(profiles.get(laboratoryId), papers.getOrDefault(laboratoryId, List.of())));
        }
        return contexts;
    }

    // LLM이 후보에 없는 연구실을 지어냈으면 버리고, 중복을 빼고 최대 3개만 남긴다
    private List<RecommendationPick> keepCandidatesOnly(List<RecommendationPick> selected, List<Long> candidateIds) {
        Set<Long> candidates = new HashSet<>(candidateIds);
        Set<Long> seen = new HashSet<>();
        List<RecommendationPick> valid = new ArrayList<>();
        for (RecommendationPick pick : selected) {
            if (!candidates.contains(pick.laboratoryId())) {
                log.warn("LLM이 후보에 없는 연구실을 추천해서 제외함 (laboratoryId={})", pick.laboratoryId());
                continue;
            }
            if (seen.add(pick.laboratoryId()) && valid.size() < MAX_RECOMMENDATIONS) {
                valid.add(pick);
            }
        }
        return valid;
    }
}
