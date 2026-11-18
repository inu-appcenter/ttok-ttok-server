package com.inuappcenter.team_2_project_server.domain.labProject.dto;

import com.inuappcenter.team_2_project_server.domain.labProject.entity.ResearchProject;

/**
 * NTIS에서 대외용으로 제공 가능한 필드만 담는다. 이 이상의 상세 정보는 ntisDetailUrl로 안내한다
 * (NTIS 상세페이지는 열람하려면 이용자가 NTIS에 직접 로그인해야 한다).
 */
public record ResearchProjectResponseDto(
        Long id,                    // pk
        String projectNumber,       // NTIS 과제고유번호
        String titleKorean,         // 과제명(한글)
        String titleEnglish,        // 과제명(영문)
        String managerName,         // 연구책임자
        String budgetProjectName,   // 사업명
        String researchAgencyName,  // 연구수행기관
        String ministryName,        // 부처명
        String projectYear,         // 기준연도
        String periodStart,         // 당해연도 연구기간
        String periodEnd,
        String totalPeriodStart,    // 전체 연구기간
        String totalPeriodEnd,
        String governmentFunds,     // 정부 연구비
        String totalFunds,          // 총 연구비
        String contentSummary,      // 연구내용 요약
        String keywordKorean,       // 한글 키워드
        String keywordEnglish,      // 영문 키워드
        boolean ongoing,            // 진행중 여부
        String ntisDetailUrl        // NTIS 상세페이지 링크
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
