package com.inuappcenter.team_2_project_server.domain.researchMetric.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.publication.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.researchMetric.client.OpenAlexClient;
import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.OpenAlexWorkResponse;
import com.inuappcenter.team_2_project_server.domain.researchMetric.entity.ResearchMetric;
import com.inuappcenter.team_2_project_server.domain.researchMetric.repository.ResearchMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAlex에서 교수별 연구 지표(h-index, 피인용 수)를 받아와 DB에 저장한다.
 * 연구실 상세페이지는 이 배치가 저장해둔 값만 읽고, 요청마다 OpenAlex를 직접 호출하지 않는다.
 * <p>
 * 교수 ↔ OpenAlex 저자 매칭 방식
 * - 이름만으로 검색하면 영문 표기 차이, 동명이인 때문에 부정확하므로 "교수의 논문"으로 후보를 좁힌다
 * - 교수의 최근 논문 DOI들로 OpenAlex 논문을 조회하고, 저자별로 몇 편에 등장하는지 집계한다
 * (교수 본인은 자기 논문 전부에 저자로 들어가 있으므로 가장 많이 등장한다)
 * - 자주 등장하는 공저자(학생, 동료 교수)를 교수로 잘못 고르지 않도록 성씨가 일치하는 저자를 우선한다
 * - 성씨가 일치하는 저자가 있으면: 그중 가장 많이 등장한 저자 (동률이면 인천대 소속으로 등장한 횟수로 비교)
 * - 없으면(외국인 교수, 특이한 영문 표기 등): 인천대 소속으로 가장 많이 등장한 저자
 * - 오매칭을 막기 위해 2편 이상에서 등장하고 2위보다 확실히 앞선 경우에만 매칭한다 (동률이면 관리자가 직접 지정)
 * - 매칭에만 소속/이름을 쓰고, 지표는 OpenAlex 저자 프로필 기준이라 인천대 이전 경력 논문까지 모두 포함된다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchMetricSyncService {

    // 매칭에 사용할 교수당 최근 논문 수 (OpenAlex에는 한 번의 요청으로 묶어서 조회)
    private static final int MATCHING_DOI_COUNT = 10;

    // 매칭 확정에 필요한 최소 등장 논문 수
    private static final int MIN_MATCH_VOTES = 2;

    private final LaboratoryRepository laboratoryRepository;
    private final PublicationRepository publicationRepository;
    private final ResearchMetricRepository researchMetricRepository;
    private final OpenAlexClient openAlexClient;

    @Scheduled(cron = "0 30 4 * * *") // 매일 새벽 4시 30분 (4시 NTIS 연구과제 동기화와 겹치지 않게)
    public void syncAll() {
        List<Professor> professors = laboratoryRepository.findAllProfessorsHavingLaboratory();
        log.info("OpenAlex 연구 지표 동기화 시작 (교수 {}명)", professors.size());

        int matchedCount = 0;
        for (Professor professor : professors) {
            try {
                if (syncOne(professor)) {
                    matchedCount++;
                }
            } catch (Exception e) {
                // 한 명이 실패해도 나머지 교수 동기화는 계속 진행
                log.error("연구 지표 동기화 실패 (professorId={})", professor.getId(), e);
            }
        }

        log.info("OpenAlex 연구 지표 동기화 종료 (저자 매칭 완료 {}명 / 전체 {}명)", matchedCount, professors.size());
    }

    /**
     * 교수 한 명의 지표를 동기화한다. 아직 저자 매칭이 안 됐으면 매칭부터 시도한다.
     *
     * @return 저자 매칭이 되어 있는지 여부
     */
    public boolean syncOne(Professor professor) {
        ResearchMetric metric = researchMetricRepository.findByProfessorId(professor.getId())
                .orElseGet(() -> researchMetricRepository.save(ResearchMetric.create(professor)));

        if (!metric.isMatched()) {
            findAuthorIdByPublications(professor).ifPresent(metric::matchAuthor);
        }

        if (!metric.isMatched()) {
            researchMetricRepository.save(metric);
            return false;
        }

        refreshMetrics(metric);
        return true;
    }

    /**
     * 매칭된 저자 ID로 OpenAlex에서 지표를 받아와 저장한다. 조회 실패 시 기존 값을 유지한다.
     */
    public void refreshMetrics(ResearchMetric metric) {
        openAlexClient.getAuthor(metric.getOpenAlexAuthorId())
                .ifPresent(author -> metric.updateMetrics(author.hIndex(), author.citedByCount(), LocalDateTime.now()));

        researchMetricRepository.save(metric);
    }

    private Optional<String> findAuthorIdByPublications(Professor professor) {
        List<String> dois = publicationRepository.findRecentDoisByProfessorId(
                professor.getId(), PageRequest.of(0, MATCHING_DOI_COUNT)
        );
        if (dois.isEmpty()) {
            return Optional.empty();
        }

        // 저자별 등장 논문 수 집계 (한 논문 안의 중복은 1번으로 센다)
        Map<String, AuthorCandidate> candidates = new HashMap<>();
        for (OpenAlexWorkResponse.Work work : openAlexClient.findWorksByDois(dois)) {
            Map<String, Boolean> authorsInWork = new HashMap<>();
            for (OpenAlexWorkResponse.Authorship authorship : work.authorshipsOrEmpty()) {
                if (authorship.author() == null || authorship.author().id() == null) {
                    continue;
                }
                authorsInWork.merge(authorship.author().id(), isInuAffiliated(authorship), Boolean::logicalOr);
                candidates.computeIfAbsent(authorship.author().id(),
                        id -> new AuthorCandidate(id, authorship.author().displayName()));
            }
            authorsInWork.forEach((authorId, inu) -> candidates.get(authorId).count(inu));
        }

        List<AuthorCandidate> surnameMatched = candidates.values().stream()
                .filter(candidate -> KoreanSurnameMatcher.matches(professor.getName(), candidate.displayName).orElse(false))
                .toList();

        // 성씨가 일치하는 저자가 있으면 그중에서, 없으면 기존처럼 인천대 소속으로 등장한 저자 중에서 고른다
        Comparator<AuthorCandidate> ranking = surnameMatched.isEmpty()
                ? Comparator.comparingInt((AuthorCandidate c) -> c.inuWorks).thenComparingInt(c -> c.works)
                : Comparator.comparingInt((AuthorCandidate c) -> c.works).thenComparingInt(c -> c.inuWorks);
        List<AuthorCandidate> ranked = (surnameMatched.isEmpty()
                ? candidates.values().stream().filter(c -> c.inuWorks > 0)
                : surnameMatched.stream())
                .sorted(ranking.reversed())
                .toList();

        if (ranked.isEmpty()) {
            return Optional.empty();
        }

        AuthorCandidate top = ranked.get(0);
        int topVotes = surnameMatched.isEmpty() ? top.inuWorks : top.works;
        boolean tied = ranked.size() > 1 && ranking.compare(top, ranked.get(1)) == 0;
        if (topVotes < MIN_MATCH_VOTES || tied) {
            log.info("OpenAlex 저자 매칭 보류 (professorId={}, 후보={})", professor.getId(),
                    ranked.stream().limit(3).map(AuthorCandidate::toString).collect(Collectors.joining(", ")));
            return Optional.empty();
        }

        return Optional.of(OpenAlexClient.toShortId(top.id));
    }

    private boolean isInuAffiliated(OpenAlexWorkResponse.Authorship authorship) {
        return authorship.author() != null
                && authorship.author().id() != null
                && authorship.institutionsOrEmpty().stream()
                .anyMatch(institution -> OpenAlexClient.INU_INSTITUTION_ID.equals(institution.id()));
    }

    // 매칭 후보 저자: 교수 논문에 등장한 횟수(works), 그중 인천대 소속으로 등장한 횟수(inuWorks)
    private static final class AuthorCandidate {
        private final String id;
        private final String displayName;
        private int works;
        private int inuWorks;

        private AuthorCandidate(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        private void count(boolean inu) {
            works++;
            if (inu) {
                inuWorks++;
            }
        }

        @Override
        public String toString() {
            return OpenAlexClient.toShortId(id) + "(" + displayName + ", " + works + "편, 인천대 " + inuWorks + "편)";
        }
    }
}
