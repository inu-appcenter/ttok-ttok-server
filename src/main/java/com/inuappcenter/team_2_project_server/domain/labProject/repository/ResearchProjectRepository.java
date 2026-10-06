package com.inuappcenter.team_2_project_server.domain.labProject.repository;

import com.inuappcenter.team_2_project_server.domain.labProject.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {

    // 동기화 시 upsert 매칭 키
    Optional<ResearchProject> findByProjectNumber(String projectNumber);

    Page<ResearchProject> findByLaboratory(Laboratory laboratory, Pageable pageable);

    // 동명이인이라 연구실 매핑이 보류된 과제 (관리자 확인용)
    Page<ResearchProject> findByLaboratoryIsNull(Pageable pageable);
}
