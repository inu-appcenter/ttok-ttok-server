package com.inuappcenter.team_2_project_server.domain.recommendation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabRecommendationRequestDto(
        @NotBlank
        @Size(max = 500)  // 질문이 길면 임베딩/LLM 비용만 늘고 검색이 정확해지지 않는다
        String question
) {
}
