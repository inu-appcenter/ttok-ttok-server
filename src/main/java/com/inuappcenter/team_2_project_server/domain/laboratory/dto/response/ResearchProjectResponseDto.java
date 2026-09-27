package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;

/**
 * NTIS에서 대외용으로 제공 가능한 필드만 담는다. 이 이상의 상세 정보는 ntisDetailUrl로 안내한다
 * (NTIS 상세페이지는 열람하려면 이용자가 NTIS에 직접 로그인해야 한다).
 */
public record ResearchProjectResponseDto(
        Long id,
        String projectNumber,
        String titleKorean,
        String titleEnglish,
        String managerName,
        String budgetProjectName,
        String researchAgencyName,
        String ministryName,
        String projectYear,
        String periodStart,
        String periodEnd,
        String totalPeriodStart,
        String totalPeriodEnd,
        String governmentFunds,
        String totalFunds,
        String contentSummary,
        String keywordKorean,
        String keywordEnglish,
        boolean ongoing,
        String ntisDetailUrl
) {
    private static final String NTIS_DETAIL_URL_FORMAT = "https://www.ntis.go.kr/project/pjtInfo.do?pjtId=%s";

    public static ResearchProjectResponseDto from(ResearchProject researchProject) {
        return new ResearchProjectResponseDto(
                researchProject.getId(),
                researchProject.getProjectNumber(),
                researchProject.getTitleKorean(),
                researchProject.getTitleEnglish(),
                researchProject.getManagerName(),
                researchProject.getBudgetProjectName(),
                researchProject.getResearchAgencyName(),
                researchProject.getMinistryName(),
                researchProject.getProjectYear(),
                researchProject.getPeriodStart(),
                researchProject.getPeriodEnd(),
                researchProject.getTotalPeriodStart(),
                researchProject.getTotalPeriodEnd(),
                researchProject.getGovernmentFunds(),
                researchProject.getTotalFunds(),
                researchProject.getContentSummary(),
                researchProject.getKeywordKorean(),
                researchProject.getKeywordEnglish(),
                researchProject.isOngoing(),
                NTIS_DETAIL_URL_FORMAT.formatted(researchProject.getProjectNumber())
        );
    }
}
