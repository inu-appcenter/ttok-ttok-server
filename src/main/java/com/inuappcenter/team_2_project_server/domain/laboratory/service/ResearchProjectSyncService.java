package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.client.NtisClient;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchProjectOwnerResolver.Candidate;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchProjectOwnerResolver.Decision;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * NTIS 국가R&D 과제검색 API로 연구실 담당교수 이름을 검색해 연구과제를 주기적으로 동기화한다.
 * 연구실 상세페이지는 이 배치가 저장해둔 값만 읽고, 요청마다 NTIS를 직접 호출하지 않는다.
 * <p>
 * NTIS는 연구책임자 이름으로만 검색할 수 있어서, 같은 이름의 교수(동명이인)가 있으면 과제가 섞여 들어온다.
 * - 동명이인이 아닌 교수: 검색된 과제를 그 교수의 연구실에 바로 매핑한다.
 * - 동명이인 교수: 과제의 과학기술표준분류/내용으로 누구의 과제인지 판별하고({@link ResearchProjectOwnerResolver}),
 *   판별하지 못하면 연구실 없이(매핑 보류) 저장해 관리자가 직접 지정하게 한다.
 * <p>
 * NTIS 호출은 트랜잭션 밖에서 하고, 저장은 교수 이름 단위로 커밋한다. (한 명이 실패해도 나머지 동기화는 계속 진행)
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
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final LaboratoryRepository laboratoryRepository;
    private final ProfessorRepository professorRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final NtisClient ntisClient;
    private final TransactionTemplate transactionTemplate;

    // NTIS 응답 중 검색어와 일치한 텍스트에 붙는 <span class="search_word">...</span> 하이라이트 마크업 제거
    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        return SEARCH_WORD_HIGHLIGHT_TAG.matcher(value).replaceAll("");
    }

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void syncAll() {
        // 교수 한 명당 연구실은 하나라서 첫 번째 연구실만 쓴다
        Map<Long, Laboratory> laboratoryByProfessorId = laboratoryRepository.findAllBy().stream()
                .filter(laboratory -> laboratory.getProfessor() != null && !isBlank(laboratory.getProfessor().getName()))
                .collect(Collectors.toMap(
                        laboratory -> laboratory.getProfessor().getId(),
                        laboratory -> laboratory,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        // 동명이인 판단은 연구실이 없는 교수까지 포함한 전체 교수 기준 (연구실 없는 동명이인의 과제도 같이 검색되기 때문)
        Map<String, List<Professor>> professorsByName = professorRepository.findAll().stream()
                .filter(professor -> !isBlank(professor.getName()))
                .collect(Collectors.groupingBy(professor -> normalizeName(professor.getName()), LinkedHashMap::new, Collectors.toList()));

        Set<String> names = laboratoryByProfessorId.values().stream()
                .map(laboratory -> normalizeName(laboratory.getProfessor().getName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.info("NTIS 연구과제 동기화 시작 (교수 이름 {}개)", names.size());

        ResearchProjectOwnerResolver resolver = new ResearchProjectOwnerResolver();
        Map<String, List<NtisProjectSearchResponse.Hit>> ambiguousHitsByName = new LinkedHashMap<>();

        // 1) 동명이인이 아닌 교수부터 처리하면서, 과제 분류를 학과별 분포로 쌓는다 (2)의 판별 근거)
        for (String name : names) {
            List<Professor> sameNameProfessors = professorsByName.getOrDefault(name, List.of());
            try {
                List<NtisProjectSearchResponse.Hit> hits = searchHits(name);
                if (sameNameProfessors.size() > 1) {
                    ambiguousHitsByName.put(name, hits);
                    continue;
                }

                Professor professor = sameNameProfessors.get(0);
                hits.forEach(hit -> resolver.learn(professor.getDepartment(), hit.newScienceClassLarges()));
                Laboratory laboratory = laboratoryByProfessorId.get(professor.getId());
                transactionTemplate.executeWithoutResult(status -> hits.forEach(hit -> upsert(laboratory, hit)));
            } catch (Exception e) {
                log.error("NTIS 연구과제 동기화 실패 (name={})", name, e);
            }
        }

        // 2) 동명이인 교수의 과제는 후보 교수 중 누구의 과제인지 판별해서 매핑
        ambiguousHitsByName.forEach((name, hits) -> {
            List<Candidate> candidates = professorsByName.get(name).stream()
                    .map(professor -> new Candidate(professor, laboratoryByProfessorId.get(professor.getId())))
                    .toList();
            try {
                transactionTemplate.executeWithoutResult(status -> hits.forEach(hit -> resolveAndUpsert(name, candidates, resolver, hit)));
            } catch (Exception e) {
                log.error("NTIS 연구과제 동기화 실패 (name={})", name, e);
            }
        });

        log.info("NTIS 연구과제 동기화 종료 (동명이인 {}개 이름은 과제 분류로 판별)", ambiguousHitsByName.size());
    }

    // 연구책임자 이름이 정확히 일치하는 과제만 사용 (검색어 하이라이트 태그와 공백은 제거하고 비교)
    private List<NtisProjectSearchResponse.Hit> searchHits(String name) {
        return ntisClient.searchByManagerName(name, 1, DISPLAY_COUNT).stream()
                .filter(hit -> !isBlank(hit.projectNumber()))
                .filter(hit -> hit.manager() != null && hit.manager().name() != null
                        && name.equals(normalizeName(clean(hit.manager().name()))))
                .toList();
    }

    private void resolveAndUpsert(
            String name,
            List<Candidate> candidates,
            ResearchProjectOwnerResolver resolver,
            NtisProjectSearchResponse.Hit hit
    ) {
        String projectText = joinNonNull(
                hit.projectTitle() == null ? null : clean(hit.projectTitle().korean()),
                hit.keyword() == null ? null : clean(hit.keyword().korean())
        );
        Decision decision = resolver.resolve(hit.newScienceClassLarges(), projectText, candidates);

        if (decision.isHeld()) {
            log.info("동명이인 연구과제 매핑 보류 (name={}, projectNumber={})", name, hit.projectNumber());
            upsert(null, hit);
            return;
        }

        Laboratory laboratory = decision.owner().laboratory();
        if (laboratory == null) {
            // 연구실이 없는 동명이인 교수의 과제 → 우리 연구실 과제가 아니므로 저장하지 않고, 이전에 잘못 저장된 것은 정리
            removeAutoAssigned(hit.projectNumber());
            return;
        }
        upsert(laboratory, hit);
    }

    private void removeAutoAssigned(String projectNumber) {
        researchProjectRepository.findByProjectNumber(projectNumber)
                .filter(existing -> !existing.isManuallyAssigned())
                .ifPresent(researchProjectRepository::delete);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeName(String name) {
        return WHITESPACE.matcher(name).replaceAll("");
    }

    private static String joinNonNull(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    // laboratory가 null이면 매핑 보류 상태로 저장. 기존 과제는 내용을 갱신하고 자동 매핑 결과도 다시 반영한다
    private void upsert(Laboratory laboratory, NtisProjectSearchResponse.Hit hit) {
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
                        existing -> {
                            existing.updateFromNtis(
                                    titleKorean, titleEnglish, managerName, budgetProjectName,
                                    researchAgencyName, ministryName, hit.projectYear(),
                                    periodStart, periodEnd, totalPeriodStart, totalPeriodEnd,
                                    hit.governmentFunds(), hit.totalFunds(), contentSummary,
                                    keywordKorean, keywordEnglish, ongoing
                            );
                            existing.assignLaboratory(laboratory);
                        },
                        () -> researchProjectRepository.save(ResearchProject.create(
                                laboratory, hit.projectNumber(), titleKorean, titleEnglish, managerName,
                                budgetProjectName, researchAgencyName, ministryName, hit.projectYear(),
                                periodStart, periodEnd, totalPeriodStart, totalPeriodEnd,
                                hit.governmentFunds(), hit.totalFunds(), contentSummary,
                                keywordKorean, keywordEnglish, ongoing
                        ))
                );
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
