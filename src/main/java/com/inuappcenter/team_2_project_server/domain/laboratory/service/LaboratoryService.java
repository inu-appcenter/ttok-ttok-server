package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCapacityUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LaboratoryService {
    private final LaboratoryRepository laboratoryRepository;
    private final ProfessorRepository professorRepository;
    private final PublicationRepository publicationRepository;
    private final ResearcherService researcherService;

    /**
     * 연구실 수동 생성 메서드
     */
    public LaboratoryResponseDto createLab(
            LaboratoryCreateRequestDto request
    ) {
        Professor professor = professorRepository.findById(request.professorId())
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));

        if (laboratoryRepository.existsByLabNameAndProfessorIdAndDepartment(
                request.labName(), request.professorId(), request.department()
        )) {
            throw new MyException(ErrorCode.DUPLICATED_LABORATORY);
        }

        Laboratory laboratory = Laboratory.create(
                request.college(),
                request.department(),
                request.labName(),
                request.location(),
                request.capacity().graduateStudentCount(),
                request.capacity().undergraduateStudentCount(),
                request.introduction(),
                professor,
                request.labUrl(),
                toResearchFieldRaw(request.researchAreas())
        );

        Laboratory savedLaboratory = laboratoryRepository.save(laboratory);

        return LaboratoryResponseDto.from(savedLaboratory);
    }

    /**
     * 연구실 단건 조회 메서드
     */
    @Transactional(readOnly = true)
    public LaboratoryResponseDto getLab(
            Long laboratoryId
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        return LaboratoryResponseDto.from(laboratory);
    }

    /**
     * 연구실 전제 조회 메서드
     */
    @Transactional(readOnly = true)
    public Page<LaboratoryResponseDto> getAllLab(Pageable pageable) {
        return laboratoryRepository.findAll(pageable)
                .map(LaboratoryResponseDto::from);
    }

    /**
     * 연구실 수정 메서드
     */
    public LaboratoryResponseDto updateLab(
            Long laboratoryId,
            LaboratoryUpdateRequestDto request,
            Member member
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        researcherService.validateAffiliation(member, laboratoryId);

        LaboratoryCapacityUpdateRequestDto capacity = request.capacity();


        laboratory.updateLab(
                request.labName(),
                request.location(),
                capacity == null ? null : capacity.graduateStudentCount(),
                capacity == null ? null : capacity.undergraduateStudentCount(),
                request.introduction(),
                request.labUrl(),
                toResearchFieldRaw(request.researchAreas())
        );

        return LaboratoryResponseDto.from(laboratory);
    }

    /**
     * 연구실 삭제 메서드
     */
    public void deleteLab(
            Member member,
            Long laboratoryId
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        researcherService.validateAffiliation(member, laboratoryId);

        laboratoryRepository.delete(laboratory);
    }

    // 요청으로 들어온 String값을 내부 ResearchFieldRaw에 저장하는 메서드
    private String toResearchFieldRaw(List<String> researchAreas) {
        if (researchAreas == null || researchAreas.isEmpty()) {
            return null;
        }

        return researchAreas.stream()
                .map(String::trim)
                .filter(area -> !area.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    @Transactional(readOnly = true)
    public Page<LaboratoryResponseDto> searchLabs(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            throw new MyException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        String trimmedKeyword = keyword.trim();

        return laboratoryRepository.findByLabNameContainingIgnoreCaseOrProfessor_NameContainingIgnoreCase(trimmedKeyword, trimmedKeyword, pageable)
                .map(LaboratoryResponseDto::from);
    }

    // 카테고리(상위 개념)로 검색하면 하위 연구분야에 속한 연구실이 전부 조회됨
    @Transactional(readOnly = true)
    public Page<LaboratoryResponseDto> searchLabsByCategory(String categoryName, Pageable pageable) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new MyException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        return laboratoryRepository.findByResearchAreaCategoryName(categoryName.trim(), pageable)
                .map(LaboratoryResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PublicationResponseDto> getLabPublications(Long laboratoryId, Pageable pageable) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        return publicationRepository.findByLaboratory(laboratory, pageable)
                .map(PublicationResponseDto::from);
    }
}
