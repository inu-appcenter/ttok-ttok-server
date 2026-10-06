package com.inuappcenter.team_2_project_server.domain.labProject.service;

import com.inuappcenter.team_2_project_server.domain.labProject.dto.ResearchProjectResponseDto;
import com.inuappcenter.team_2_project_server.domain.labProject.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.labProject.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResearchProjectService {
    private final LaboratoryRepository laboratoryRepository;
    private final ResearchProjectRepository researchProjectRepository;

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponseDto> getLabResearchProjects(Long laboratoryId, Pageable pageable) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        return researchProjectRepository.findByLaboratory(laboratory, pageable)
                .map(ResearchProjectResponseDto::from);
    }

    // 동명이인 교수의 과제라 동기화 배치가 연구실 매핑을 보류한 과제 목록 (관리자 전용)
    @Transactional(readOnly = true)
    public Page<ResearchProjectResponseDto> getPendingResearchProjects(Pageable pageable) {
        return researchProjectRepository.findByLaboratoryIsNull(pageable)
                .map(ResearchProjectResponseDto::from);
    }

    /**
     * 관리자가 연구과제의 연구실을 직접 지정하는 메서드
     * 매핑이 보류됐거나 잘못 매핑된 과제를 바로잡을 때 사용 (이후 동기화 배치의 자동 매핑이 덮어쓰지 않음)
     */
    @Transactional
    public ResearchProjectResponseDto assignResearchProjectLaboratory(Long researchProjectId, Long laboratoryId) {
        ResearchProject researchProject = researchProjectRepository.findById(researchProjectId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCH_PROJECT_NOT_FOUND));
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        researchProject.assignLaboratoryManually(laboratory);
        return ResearchProjectResponseDto.from(researchProject);
    }
}
