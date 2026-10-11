package com.inuappcenter.team_2_project_server.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 연구실 추천 키워드 요청. interest는 관심 있는 연구를 자유롭게 적은 글 (연구실 추천 요청의 interest와 같은 값)
 */
public record LabKeywordSuggestionRequestDto(
        @NotBlank
        @Size(max = 300)
        String interest
) {
}
