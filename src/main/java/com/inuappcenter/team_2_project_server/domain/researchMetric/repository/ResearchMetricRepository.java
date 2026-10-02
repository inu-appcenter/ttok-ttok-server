package com.inuappcenter.team_2_project_server.domain.researchMetric.repository;

import com.inuappcenter.team_2_project_server.domain.researchMetric.entity.ResearchMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResearchMetricRepository extends JpaRepository<ResearchMetric, Long> {

    // 연구실 → 교수 → 지표 조회
    Optional<ResearchMetric> findByProfessorId(Long professorId);

    // 지표 동기화 대상 (OpenAlex 저자와 매칭이 끝난 교수만)
    List<ResearchMetric> findAllByOpenAlexAuthorIdIsNotNull();
}
