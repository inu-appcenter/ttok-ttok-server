package com.inuappcenter.team_2_project_server.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 연구실 추천 요청
 * - interest: 관심 있는 연구를 자유롭게 적은 글
 * - selectedKeywords: 키워드 추천 API로 받은 카테고리 중 고른 것 + 직접 입력한 키워드. 고르지 않았으면 비어 있어도 된다
 */
public record LabRecommendationRequestDto(
        @NotBlank
        @Size(max = 300)
        String interest,

        @Size(max = 3)
        List<@NotBlank @Size(max = 30) String> selectedKeywords
) {
}
