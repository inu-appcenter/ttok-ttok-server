package com.inuappcenter.team_2_project_server.domain.researchArea.dto;

import com.inuappcenter.team_2_project_server.domain.researchArea.entity.ResearchAreaCategory;

public record ResearchAreaCategoryResponseDto(
        String categoryName
) {
    public static ResearchAreaCategoryResponseDto from(ResearchAreaCategory researchAreaCategory) {
        return new ResearchAreaCategoryResponseDto(
                researchAreaCategory.getCategoryName()
        );
    }
}
