package com.inuappcenter.team_2_project_server.ai;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.ai.client.FactChatClient;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.LabCandidateContextRow;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.LabCandidateRow;
import com.inuappcenter.team_2_project_server.domain.ai.repository.LabSearchDocumentRepository;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.LabRecommendationService;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.RecommendationPromptBuilder;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LabRecommendationServiceTest {

    private static final String QUESTION = "그린수소 촉매 연구";
    private final Map<Long, Laboratory> laboratories = new HashMap<>();
    private LabSearchDocumentRepository labSearchDocumentRepository;
    private FactChatClient factChatClient;
    private LabRecommendationService labRecommendationService;

    @BeforeEach
    void setUp() {
        labSearchDocumentRepository = mock(LabSearchDocumentRepository.class);
        LaboratoryRepository laboratoryRepository = mock(LaboratoryRepository.class);
        factChatClient = mock(FactChatClient.class);
        labRecommendationService = new LabRecommendationService(
                labSearchDocumentRepository,
                laboratoryRepository,
                factChatClient,
                new RecommendationPromptBuilder(JsonMapper.builder().build()),
                "embedding-model",
                "chat-model"
        );

        // 후보 연구실 5개 (검색 점수 순: 1, 2, 3, 4, 5)
        for (long id = 1; id <= 5; id++) {
            laboratory(id);
        }
        given(factChatClient.embed(eq("embedding-model"), eq(List.of(QUESTION))))
                .willReturn(Optional.of(List.of(new float[]{0.5f, -0.25f})));
        given(labSearchDocumentRepository.findCandidates(anyString(), anyDouble(), anyInt(), anyInt()))
                .willReturn(List.of(candidate(1), candidate(2), candidate(3), candidate(4), candidate(5)));
        given(labSearchDocumentRepository.findCandidateContexts(anyString(), anyCollection(), anyInt()))
                .willReturn(List.of(context(1, "PROFILE", "연구실1 프로필"), context(1, "PUBLICATION", "연구실1 논문")));
        given(laboratoryRepository.findWithProfessorByIdIn(anyCollection())).willAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().filter(laboratories::containsKey).map(laboratories::get).toList();
        });
    }

    @Test
    void recommends_labs_chosen_by_llm_in_llm_order_with_reasons() {
        llmAnswers("{\"recommendations\": [{\"lab_id\": 3, \"reason\": \"이유3\"}, {\"lab_id\": 1, \"reason\": \"이유1\"}]}");

        LabRecommendationResponseDto response = labRecommendationService.recommend("  " + QUESTION + "  ");

        assertThat(response.noMatch()).isFalse();
        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::laboratoryId).containsExactly(3L, 1L);
        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::reason).containsExactly("이유3", "이유1");
        // 연구실 정보는 LLM 답이 아니라 DB에서 채운다
        assertThat(response.recommendations().getFirst().labName()).isEqualTo("연구실3");
        assertThat(response.recommendations().getFirst().professorName()).isEqualTo("교수3");
    }

    @Test
    void searches_with_question_vector_and_sends_candidates_to_llm() {
        llmAnswers("{\"recommendations\": []}");

        labRecommendationService.recommend(QUESTION);

        verify(labSearchDocumentRepository).findCandidates(eq("[0.5,-0.25]"), eq(0.7), eq(3), eq(20));
        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(factChatClient).chat(eq("chat-model"), eq(RecommendationPromptBuilder.SYSTEM_PROMPT), userMessage.capture());
        assertThat(userMessage.getValue()).contains(QUESTION, "[lab_id=1]\n연구실1 프로필\n질문과 관련 있는 논문: 연구실1 논문");
    }

    @Test
    void drops_labs_not_in_candidates_and_duplicates_and_keeps_at_most_three() {
        llmAnswers("""
                {"recommendations": [
                  {"lab_id": 999, "reason": "후보에 없는 연구실"},
                  {"lab_id": 2, "reason": "a"}, {"lab_id": 2, "reason": "중복"},
                  {"lab_id": 4, "reason": "b"}, {"lab_id": 5, "reason": "c"}, {"lab_id": 1, "reason": "4번째"}
                ]}""");

        LabRecommendationResponseDto response = labRecommendationService.recommend(QUESTION);

        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::laboratoryId).containsExactly(2L, 4L, 5L);
    }

    @Test
    void no_match_when_llm_finds_no_suitable_lab() {
        llmAnswers("{\"recommendations\": []}");

        LabRecommendationResponseDto response = labRecommendationService.recommend(QUESTION);

        assertThat(response.noMatch()).isTrue();
        assertThat(response.recommendations()).isEmpty();
    }

    @Test
    void falls_back_to_top3_search_results_without_reason_when_llm_call_fails() {
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.empty());

        LabRecommendationResponseDto response = labRecommendationService.recommend(QUESTION);

        assertThat(response.noMatch()).isFalse();
        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::laboratoryId).containsExactly(1L, 2L, 3L);
        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::reason).containsOnlyNulls();
    }

    @Test
    void falls_back_when_llm_answer_is_not_json() {
        llmAnswers("추천할 연구실은 1번입니다.");

        LabRecommendationResponseDto response = labRecommendationService.recommend(QUESTION);

        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::laboratoryId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void skips_lab_deleted_after_indexing() {
        llmAnswers("{\"recommendations\": [{\"lab_id\": 2, \"reason\": \"a\"}, {\"lab_id\": 3, \"reason\": \"b\"}]}");
        laboratories.remove(2L);

        LabRecommendationResponseDto response = labRecommendationService.recommend(QUESTION);

        assertThat(response.recommendations()).extracting(LabRecommendationResponseDto.Item::laboratoryId).containsExactly(3L);
    }

    @Test
    void unavailable_when_question_embedding_fails() {
        given(factChatClient.embed(anyString(), anyList())).willReturn(Optional.empty());

        assertThatThrownBy(() -> labRecommendationService.recommend(QUESTION))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOMMENDATION_UNAVAILABLE);
    }

    @Test
    void unavailable_when_search_index_is_empty() {
        given(labSearchDocumentRepository.findCandidates(anyString(), anyDouble(), anyInt(), anyInt())).willReturn(List.of());

        assertThatThrownBy(() -> labRecommendationService.recommend(QUESTION))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOMMENDATION_UNAVAILABLE);
    }

    private void llmAnswers(String answer) {
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.of(answer));
    }

    private void laboratory(long id) {
        Professor professor = Professor.create("교수" + id, "교수", Department.COMPUTER_ENGINEERING, null, null);
        Laboratory laboratory = Laboratory.create(
                professor.getCollege(), Department.COMPUTER_ENGINEERING, "연구실" + id, null, 0, 0, null, professor, null, null
        );
        ReflectionTestUtils.setField(laboratory, "id", id);
        laboratories.put(id, laboratory);
    }

    private LabCandidateRow candidate(long laboratoryId) {
        return new CandidateRow(laboratoryId, 1.0 / laboratoryId);
    }

    private LabCandidateContextRow context(long laboratoryId, String sourceType, String content) {
        return new ContextRow(laboratoryId, sourceType, content);
    }

    private record CandidateRow(Long getLaboratoryId, Double getScore) implements LabCandidateRow {
    }

    private record ContextRow(Long getLaboratoryId, String getSourceType, String getContent) implements LabCandidateContextRow {
    }
}
