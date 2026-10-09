package com.inuappcenter.team_2_project_server.domain.ai.service.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 연구실 추천 2단계(LLM 선택)의 프롬프트를 만들고, LLM 답변(JSON)을 해석한다.
 * 프롬프트는 inu-rag/eval/rerank_eval.py 실험과 같다. 바꾸면 평가셋으로 다시 채점해서 점수를 확인해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationPromptBuilder {

    public static final String SYSTEM_PROMPT = """
            당신은 인천대학교 학생에게 연구실을 추천하는 도우미입니다.
            
            규칙:
            1. 반드시 아래 [후보 연구실] 목록 안에서만 고릅니다. 목록에 없는 연구실이나 ID를 만들어 내지 않습니다.
            2. 학생의 관심사와 연구 내용이 실제로 맞는 연구실을 최대 3개, 잘 맞는 순서대로 고릅니다.
            3. 질문에 학과 같은 조건이 있으면 그 조건을 만족하는 연구실만 고릅니다.
            4. 단어가 비슷할 뿐 연구 내용이 다르면 고르지 않습니다. 잘 맞는 연구실이 없으면 빈 목록을 돌려줍니다.
            5. 추천 이유는 후보 정보에 적힌 내용만 근거로 한두 문장으로 씁니다.
            
            아래 형식의 JSON만 출력합니다. 다른 글은 쓰지 않습니다.
            {"recommendations": [{"lab_id": 숫자, "reason": "추천 이유"}]}""";

    // LLM이 JSON 앞뒤에 설명이나 ```json 표시를 붙여도 JSON 부분만 꺼낸다
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    /**
     * 사용자 메시지: 학생 질문 + 후보 연구실 목록 (검색 점수 순)
     *
     * @param candidateIds 후보 연구실 id (검색 점수 높은 순)
     * @param contexts     연구실 id -> 프롬프트에 넣을 프로필과 관련 논문
     */
    public String buildUserMessage(String question, List<Long> candidateIds, Map<Long, CandidateContext> contexts) {
        StringBuilder message = new StringBuilder()
                .append("[학생 질문]\n").append(question).append("\n\n")
                .append("[후보 연구실]");

        for (Long laboratoryId : candidateIds) {
            CandidateContext context = contexts.get(laboratoryId);
            if (context == null || context.profile() == null) {
                continue;
            }
            message.append("\n\n[lab_id=").append(laboratoryId).append("]\n").append(context.profile());
            if (!context.papers().isEmpty()) {
                message.append("\n질문과 관련 있는 논문: ").append(String.join(" / ", context.papers()));
            }
        }
        return message.toString();
    }

    /**
     * LLM 답변에서 추천 목록을 꺼낸다. JSON이 아니거나 형식이 틀리면 Optional.empty()
     * (빈 목록은 "맞는 연구실 없음"이라는 정상 답변이라 empty와 구분한다)
     */
    public Optional<List<RecommendationPick>> parse(String llmAnswer) {
        if (llmAnswer == null) {
            return Optional.empty();
        }
        Matcher matcher = JSON_OBJECT.matcher(llmAnswer);
        if (!matcher.find()) {
            log.warn("LLM 추천 답변에 JSON이 없음: {}", abbreviate(llmAnswer));
            return Optional.empty();
        }

        try {
            LlmAnswer answer = objectMapper.readValue(matcher.group(), LlmAnswer.class);
            if (answer.recommendations() == null) {
                return Optional.empty();
            }
            return Optional.of(answer.recommendations().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.labId() != null)
                    .map(item -> new RecommendationPick(item.labId(), item.reason()))
                    .toList());
        } catch (RuntimeException e) {
            log.warn("LLM 추천 답변 JSON 해석 실패: {}", abbreviate(llmAnswer));
            return Optional.empty();
        }
    }

    private String abbreviate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    /**
     * 프롬프트에 넣을 후보 연구실 정보
     *
     * @param profile 연구실 프로필 원문
     * @param papers  그 연구실 논문 중 질문과 가까운 논문 원문 (가까운 순)
     */
    public record CandidateContext(String profile, List<String> papers) {
    }

    /**
     * LLM이 고른 연구실 하나
     *
     * @param reason LLM이 쓴 추천 이유. LLM 단계가 실패해 검색 결과로 대체한 경우 null
     */
    public record RecommendationPick(Long laboratoryId, String reason) {
    }

    // LLM 답변 JSON 형식: {"recommendations": [{"lab_id": 12, "reason": "..."}]}
    record LlmAnswer(List<Item> recommendations) {
        record Item(@JsonProperty("lab_id") Long labId, String reason) {
        }
    }
}
