package com.inuappcenter.team_2_project_server.domain.researchMetric.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAlex 논문 검색 응답 (GET /works?filter=doi:...)
 * 교수 ↔ OpenAlex 저자 매칭을 위해 저자 목록(authorships)만 받아온다
 */
public record OpenAlexWorkResponse(
        List<Work> results
) {
    public record Work(
            String id,
            String doi,
            List<Authorship> authorships
    ) {
        public List<Authorship> authorshipsOrEmpty() {
            return authorships == null ? List.of() : authorships;
        }
    }

    public record Authorship(
            Author author,
            List<Institution> institutions,
            @JsonProperty("raw_author_name")
            String rawAuthorName    // 논문에 실제로 적힌 저자 표기
    ) {
        public List<Institution> institutionsOrEmpty() {
            return institutions == null ? List.of() : institutions;
        }
    }

    public record Author(
            String id,
            @JsonProperty("display_name")
            String displayName
    ) {
    }

    public record Institution(
            String id,
            @JsonProperty("display_name")
            String displayName
    ) {
    }

    public List<Work> resultsOrEmpty() {
        return results == null ? List.of() : results;
    }
}
