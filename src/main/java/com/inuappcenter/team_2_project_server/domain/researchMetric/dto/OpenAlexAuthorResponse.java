package com.inuappcenter.team_2_project_server.domain.researchMetric.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenAlex 저자 조회 응답 (GET /authors/{id})
 * 필요한 필드만 select로 받아오며, 나머지 필드는 무시한다
 */
public record OpenAlexAuthorResponse(
        String id,                      // https://openalex.org/A5067381195 형태
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty("summary_stats")
        SummaryStats summaryStats,
        @JsonProperty("cited_by_count")
        Integer citedByCount
) {
    public record SummaryStats(
            @JsonProperty("h_index")
            Integer hIndex
    ) {
    }

    public Integer hIndex() {
        return summaryStats == null ? null : summaryStats.hIndex();
    }
}
