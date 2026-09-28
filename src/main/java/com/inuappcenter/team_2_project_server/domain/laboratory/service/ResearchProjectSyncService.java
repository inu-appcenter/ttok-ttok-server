package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.client.NtisClient;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * NTIS 국가R&D 과제검색 API로 각 연구실의 담당교수 이름을 검색해 연구과제를 주기적으로 동기화한다.
 * 연구실 상세페이지는 이 배치가 저장해둔 값만 읽고, 요청마다 NTIS를 직접 호출하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchProjectSyncService {

    // 한 교수당 가져올 최대 과제 수. 한 페이지만 받아오며, 전체 페이지네이션은 지원하지 않는다
    private static final int DISPLAY_COUNT = 30;

    // NTIS는 검색어와 일치하는 텍스트를 <span class="search_word">...</span>로 감싸서 내려준다.
    // 담당교수 이름으로 검색하므로 Manager.Name, ResearchAgency.Name 등에서 빈번히 나타난다
    private static final Pattern SEARCH_WORD_HIGHLIGHT_TAG = Pattern.compile("</?span[^>]*>");

    private final LaboratoryRepository laboratoryRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final NtisClient ntisClient;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void syncAll() {
        List<Laboratory> laboratories = laboratoryRepository.findAll();
        log.info("NTIS 연구과제 동기화 시작 (연구실 {}개)", laboratories.size());

        for (Laboratory laboratory : laboratories) {
            syncOne(laboratory);
        }

        log.info("NTIS 연구과제 동기화 종료");
    }

    public void syncOne(Laboratory laboratory) {
        Professor professor = laboratory.getProfessor();
        if (professor == null || professor.getName() == null || professor.getName().isBlank()) {
            return;
        }

        List<NtisProjectSearchResponse.Hit> hits = ntisClient.searchByManagerName(
                professor.getName(), 1, DISPLAY_COUNT
        );

        for (NtisProjectSearchResponse.Hit hit : hits) {
            upsert(laboratory, hit);
        }
    }

    private void upsert(Laboratory laboratory, NtisProjectSearchResponse.Hit hit) {
        if (hit.projectNumber() == null || hit.projectNumber().isBlank()) {
            return;
        }

        NtisProjectSearchResponse.ProjectPeriod period = hit.projectPeriod();
        boolean ongoing = isOngoing(period);

        String titleKorean = clean(hit.projectTitle() == null ? null : hit.projectTitle().korean());
        String titleEnglish = clean(hit.projectTitle() == null ? null : hit.projectTitle().english());
        String managerName = clean(hit.manager() == null ? null : hit.manager().name());
        String budgetProjectName = clean(hit.budgetProject() == null ? null : hit.budgetProject().name());
        String researchAgencyName = clean(hit.researchAgency() == null ? null : hit.researchAgency().name());
        String ministryName = clean(hit.ministry() == null ? null : hit.ministry().name());
        String periodStart = period == null ? null : period.start();
        String periodEnd = period == null ? null : period.end();
        String totalPeriodStart = period == null ? null : period.totalStart();
        String totalPeriodEnd = period == null ? null : period.totalEnd();
        String keywordKorean = clean(hit.keyword() == null ? null : hit.keyword().korean());
        String keywordEnglish = clean(hit.keyword() == null ? null : hit.keyword().english());
        String contentSummary = clean(hit.contentSummary());

        researchProjectRepository.findByProjectNumber(hit.projectNumber())
                .ifPresentOrElse(
                        existing -> existing.updateFromNtis(
                                titleKorean, titleEnglish, managerName, budgetProjectName,
                                researchAgencyName, ministryName, hit.projectYear(),
                                periodStart, periodEnd, totalPeriodStart, totalPeriodEnd,
                                hit.governmentFunds(), hit.totalFunds(), contentSummary,
                                keywordKorean, keywordEnglish, ongoing
                        ),
                        () -> researchProjectRepository.save(ResearchProject.create(
                                laboratory, hit.projectNumber(), titleKorean, titleEnglish, managerName,
                                budgetProjectName, researchAgencyName, ministryName, hit.projectYear(),
                                periodStart, periodEnd, totalPeriodStart, totalPeriodEnd,
                                hit.governmentFunds(), hit.totalFunds(), contentSummary,
                                keywordKorean, keywordEnglish, ongoing
                        ))
                );
    }

    // NTIS 응답 중 검색어와 일치한 텍스트에 붙는 <span class="search_word">...</span> 하이라이트 마크업 제거
    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        return SEARCH_WORD_HIGHLIGHT_TAG.matcher(value).replaceAll("");
    }

    // totalEnd 형식: "2020-06-30 00:00:00.0"
    private boolean isOngoing(NtisProjectSearchResponse.ProjectPeriod period) {
        if (period == null || period.totalEnd() == null) {
            return false;
        }

        try {
            String datePart = period.totalEnd().split(" ")[0];
            LocalDate totalEnd = LocalDate.parse(datePart);
            return !totalEnd.isBefore(LocalDate.now());
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            log.warn("총연구기간 종료일 파싱 실패: {}", period.totalEnd());
            return false;
        }
    }
}
