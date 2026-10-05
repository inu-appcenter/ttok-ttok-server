package com.inuappcenter.team_2_project_server.domain.laboratory.entity;

import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * NTIS 국가R&D 과제검색 API에서 주기적으로 가져와 저장하는 연구과제.
 * 여기 담는 필드는 NTIS 매뉴얼상 "대외용(웹/모바일) 서비스에 제공 가능한 항목"으로 한정한다.
 * 그 이상 상세 정보는 담지 않고, 과제고유번호로 NTIS 상세페이지 링크만 제공한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "research_project",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_research_project_project_number", columnNames = "project_number")
        }
)
public class ResearchProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "research_project_id")
    private Long id;

    // 동명이인 교수의 과제라 어느 연구실 과제인지 판별하지 못하면 null(매핑 보류)로 저장하고, 관리자가 직접 지정한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    // 관리자가 직접 연구실을 지정한 과제. 이후 동기화 배치의 자동 매핑이 덮어쓰지 않는다
    @Column(name = "manually_assigned", nullable = false)
    private boolean manuallyAssigned;

    // NTIS 과제고유번호. 상세페이지 링크(https://www.ntis.go.kr/project/pjtInfo.do?pjtId={projectNumber}) 생성 및 재동기화 매칭 키로 쓴다
    @Column(name = "project_number", nullable = false)
    private String projectNumber;

    @Column(name = "title_korean", columnDefinition = "TEXT")
    private String titleKorean;

    @Column(name = "title_english", columnDefinition = "TEXT")
    private String titleEnglish;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "budget_project_name")
    private String budgetProjectName;

    @Column(name = "research_agency_name")
    private String researchAgencyName;

    @Column(name = "ministry_name")
    private String ministryName;

    @Column(name = "project_year")
    private String projectYear;

    // 당해연도 연구기간 (yyyyMMdd 원문 그대로 저장)
    @Column(name = "period_start")
    private String periodStart;

    @Column(name = "period_end")
    private String periodEnd;

    // 총연구기간 (원문 그대로 저장, ongoing 판단은 별도 계산해서 저장)
    @Column(name = "total_period_start")
    private String totalPeriodStart;

    @Column(name = "total_period_end")
    private String totalPeriodEnd;

    @Column(name = "government_funds")
    private String governmentFunds;

    @Column(name = "total_funds")
    private String totalFunds;

    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary;

    @Column(name = "keyword_korean", columnDefinition = "TEXT")
    private String keywordKorean;

    @Column(name = "keyword_english", columnDefinition = "TEXT")
    private String keywordEnglish;

    // 동기화 시점에 totalPeriodEnd를 기준으로 계산해서 저장 (매 조회마다 파싱/계산하지 않기 위함)
    @Column(name = "ongoing", nullable = false)
    private boolean ongoing;

    private ResearchProject(
            Laboratory laboratory,
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
            boolean ongoing
    ) {
        this.laboratory = laboratory;
        this.projectNumber = projectNumber;
        this.titleKorean = titleKorean;
        this.titleEnglish = titleEnglish;
        this.managerName = managerName;
        this.budgetProjectName = budgetProjectName;
        this.researchAgencyName = researchAgencyName;
        this.ministryName = ministryName;
        this.projectYear = projectYear;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalPeriodStart = totalPeriodStart;
        this.totalPeriodEnd = totalPeriodEnd;
        this.governmentFunds = governmentFunds;
        this.totalFunds = totalFunds;
        this.contentSummary = contentSummary;
        this.keywordKorean = keywordKorean;
        this.keywordEnglish = keywordEnglish;
        this.ongoing = ongoing;
    }

    public static ResearchProject create(
            Laboratory laboratory,
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
            boolean ongoing
    ) {
        return new ResearchProject(
                laboratory, projectNumber, titleKorean, titleEnglish, managerName, budgetProjectName,
                researchAgencyName, ministryName, projectYear, periodStart, periodEnd,
                totalPeriodStart, totalPeriodEnd, governmentFunds, totalFunds,
                contentSummary, keywordKorean, keywordEnglish, ongoing
        );
    }

    // 동기화 배치의 자동 매핑 결과 반영 (laboratory가 null이면 매핑 보류). 관리자가 직접 지정한 과제는 덮어쓰지 않는다
    public void assignLaboratory(Laboratory laboratory) {
        if (manuallyAssigned) {
            return;
        }
        this.laboratory = laboratory;
    }

    // 관리자가 직접 연구실을 지정 (자동 매핑이 보류됐거나 잘못 매핑된 과제를 바로잡을 때 사용)
    public void assignLaboratoryManually(Laboratory laboratory) {
        this.laboratory = laboratory;
        this.manuallyAssigned = true;
    }

    // 재동기화 시 최신 값으로 갱신
    public void updateFromNtis(
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
            boolean ongoing
    ) {
        this.titleKorean = titleKorean;
        this.titleEnglish = titleEnglish;
        this.managerName = managerName;
        this.budgetProjectName = budgetProjectName;
        this.researchAgencyName = researchAgencyName;
        this.ministryName = ministryName;
        this.projectYear = projectYear;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalPeriodStart = totalPeriodStart;
        this.totalPeriodEnd = totalPeriodEnd;
        this.governmentFunds = governmentFunds;
        this.totalFunds = totalFunds;
        this.contentSummary = contentSummary;
        this.keywordKorean = keywordKorean;
        this.keywordEnglish = keywordEnglish;
        this.ongoing = ongoing;
    }
}
