package com.inuappcenter.team_2_project_server.domain.ai.dto.response;

import java.util.List;

/**
 * 연구실 추천 키워드 응답
 * - keywords: 연구분야 카테고리명, 잘 맞는 순서대로 최대 5개. 맞는 연구실을 찾지 못하면 빈 목록
 */
public record LabKeywordSuggestionResponseDto(
        List<String> keywords
) {
}
