package com.inuappcenter.team_2_project_server.recommendation;

import com.inuappcenter.team_2_project_server.domain.recommendation.service.RecommendationPromptBuilder;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.RecommendationPromptBuilder.CandidateContext;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.RecommendationPromptBuilder.RecommendationPick;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPromptBuilderTest {

    private final RecommendationPromptBuilder promptBuilder = new RecommendationPromptBuilder(JsonMapper.builder().build());

    // ===== 프롬프트 만들기 =====

    @Test
    void user_message_lists_candidates_in_search_order_with_related_papers() {
        String message = promptBuilder.buildUserMessage(
                "수소 연료전지 연구",
                List.of(9L, 77L),
                Map.of(
                        9L, new CandidateContext("멀티스케일아키텍처링연구실 (기계공학과, 김상문 교수)", List.of("논문A. 저널", "논문B")),
                        77L, new CandidateContext("전기 화학 에너지 디바이스 연구실", List.of())
                )
        );

        assertThat(message).startsWith("[학생 질문]\n수소 연료전지 연구\n\n[후보 연구실]");
        assertThat(message).contains("[lab_id=9]\n멀티스케일아키텍처링연구실 (기계공학과, 김상문 교수)\n질문과 관련 있는 논문: 논문A. 저널 / 논문B");
        assertThat(message.indexOf("[lab_id=9]")).isLessThan(message.indexOf("[lab_id=77]"));
        // 관련 논문이 없는 연구실은 논문 줄을 붙이지 않는다
        assertThat(message).endsWith("[lab_id=77]\n전기 화학 에너지 디바이스 연구실");
    }

    @Test
    void user_message_skips_candidate_without_profile() {
        String message = promptBuilder.buildUserMessage(
                "질문",
                List.of(1L, 2L),
                Map.of(1L, new CandidateContext("연구실1", List.of()), 2L, new CandidateContext(null, List.of("논문")))
        );

        assertThat(message).contains("[lab_id=1]").doesNotContain("[lab_id=2]");
    }

    // ===== LLM 답변 해석 =====

    @Test
    void parses_recommendations_in_order() {
        Optional<List<RecommendationPick>> picks = promptBuilder.parse("""
                {"recommendations": [{"lab_id": 157, "reason": "수전해 촉매"}, {"lab_id": 68, "reason": "CO2 전환"}]}""");

        assertThat(picks).hasValueSatisfying(list -> assertThat(list).containsExactly(
                new RecommendationPick(157L, "수전해 촉매"),
                new RecommendationPick(68L, "CO2 전환")
        ));
    }

    @Test
    void parses_json_wrapped_in_code_fence_and_text() {
        Optional<List<RecommendationPick>> picks = promptBuilder.parse("""
                추천 결과입니다.
                ```json
                {"recommendations": [{"lab_id": 9, "reason": "연료전지"}]}
                ```""");

        assertThat(picks).hasValueSatisfying(list -> assertThat(list).containsExactly(new RecommendationPick(9L, "연료전지")));
    }

    @Test
    void empty_recommendations_is_valid_answer_meaning_no_match() {
        assertThat(promptBuilder.parse("{\"recommendations\": []}")).hasValue(List.of());
    }

    @Test
    void skips_item_without_lab_id() {
        Optional<List<RecommendationPick>> picks = promptBuilder.parse("""
                {"recommendations": [{"reason": "id 없음"}, {"lab_id": 3, "reason": "정상"}]}""");

        assertThat(picks).hasValueSatisfying(list -> assertThat(list).containsExactly(new RecommendationPick(3L, "정상")));
    }

    @Test
    void returns_empty_when_answer_is_not_json() {
        assertThat(promptBuilder.parse("죄송하지만 추천할 수 없습니다.")).isEmpty();
        assertThat(promptBuilder.parse("{\"recommendations\": [{\"lab_id\": ")).isEmpty();
        assertThat(promptBuilder.parse("{\"other\": 1}")).isEmpty();
        assertThat(promptBuilder.parse(null)).isEmpty();
    }
}
