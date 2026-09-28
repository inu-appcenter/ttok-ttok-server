package com.inuappcenter.team_2_project_server.domain.laboratory.repository;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {

    // 동기화 시 upsert 매칭 키
    Optional<ResearchProject> findByProjectNumber(String projectNumber);

    Page<ResearchProject> findByLaboratory(Laboratory laboratory, Pageable pageable);
}
