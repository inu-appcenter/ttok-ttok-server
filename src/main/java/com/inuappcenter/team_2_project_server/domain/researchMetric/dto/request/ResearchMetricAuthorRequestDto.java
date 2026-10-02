package com.inuappcenter.team_2_project_server.domain.researchMetric.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResearchMetricAuthorRequestDto(
        @NotBlank
        String openAlexAuthorId     // "A5067381195" 또는 "https://openalex.org/A5067381195"
) {
}
