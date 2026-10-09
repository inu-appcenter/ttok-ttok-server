package com.inuappcenter.team_2_project_server.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 연구실 추천 요청. 기존 챗봇 API와 같은 필드명(message)을 써서 프론트의 요청 코드는 그대로 둘 수 있다.
 */
public record LabRecommendationRequestDto(
        @NotBlank
        @Size(max = 500)  // 질문이 길면 임베딩/LLM 비용만 늘고 검색이 정확해지지 않는다
        String message
) {
}
