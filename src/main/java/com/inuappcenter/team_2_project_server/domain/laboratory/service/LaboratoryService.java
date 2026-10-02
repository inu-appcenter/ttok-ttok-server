package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCapacityUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.*;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LaboratoryService {
    private final LaboratoryRepository laboratoryRepository;
    private final ProfessorService professorService;
    private final PublicationRepository publicationRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final ResearchProjectSyncService researchProjectSyncService;
    private final ResearcherService researcherService;

    /**
     * 연구실 수동 생성 메서드. 로그인한 계정과 연동된 교수 본인 명의로만 생성할 수 있다
     * (엑셀 데이터엔 있지만 아직 연구실이 없는 교수가 직접 개설하는 용도)
     */
    public LaboratoryResponseDto createLab(
            Member member,
            LaboratoryCreateRequestDto request
    ) {
        Professor professor = professorService.getByMemberId(member.getId());

        if (laboratoryRepository.existsByProfessorId(professor.getId())) {
            throw new MyException(ErrorCode.PROFESSOR_ALREADY_HAS_LABORATORY);
        }

        if (laboratoryRepository.existsByLabNameAndProfessorIdAndDepartment(
                request.labName(), professor.getId(), request.department()
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

        validateAccess(member, laboratoryId);

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

        validateAccess(member, laboratoryId);

        laboratoryRepository.delete(laboratory);
    }

    /**
     * 이 연구실을 수정/삭제할 권한이 있는지 검증 — 소속 연구자이거나, 연동된 담당 교수 본인이면 통과
     */
    private void validateAccess(Member member, Long laboratoryId) {
        boolean isResearcher = researcherService.isAffiliated(member.getId(), laboratoryId);
        boolean isOwnerProfessor = laboratoryRepository.existsByIdAndProfessor_MemberId(laboratoryId, member.getId());

        if (!isResearcher && !isOwnerProfessor) {
            throw new MyException(ErrorCode.INVALID_LAB_ACCESS);
        }
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

    // 모든 조건은 선택값이며, 비어 있으면 해당 조건은 '전체'로 처리됨
    // 단과대/학과는 한글 이름(예: 공과대학, 전자공학부)으로 받아 enum으로 변환 (없는 이름이면 INVALID_INPUT)
    @Transactional(readOnly = true)
    public Page<LaboratoryResponseDto> searchLabs(
            String keyword,
            String collegeName,
            String departmentName,
            String researchArea,
            Pageable pageable
    ) {
        College college = isBlank(collegeName) ? null : College.fromCollegeName(collegeName.trim());
        Department department = isBlank(departmentName) ? null : Department.fromDepartmentName(departmentName.trim());

        return laboratoryRepository.searchByFilter(escapeLike(trimOrEmpty(keyword)), college, department, trimOrEmpty(researchArea), pageable)
                .map(LaboratoryResponseDto::from);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    // like 검색에서 %, _ 가 와일드카드로 동작하지 않도록 이스케이프 (escape 문자는 \)
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponseDto> getLabResearchProjects(Long laboratoryId, Pageable pageable) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        return researchProjectRepository.findByLaboratory(laboratory, pageable)
                .map(ResearchProjectResponseDto::from);
    }

    /**
     * 내부 호출용 엔티티 조회 메서드
     */
    @Transactional(readOnly = true)
    public Laboratory getLaboratoryEntity(Long laboratoryId) {
        return laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));
    }

    /**
     * 단과대/학과별로 연구실 갯수 조회
     */
    @Transactional(readOnly = true)
    public List<LabCountByCollegeResponseDto> getLabByCollegeDeptCount() {
        // (단과대, 학과, 개수) row를 가져옴
        List<LabCollegeDeptCountRow> rows = laboratoryRepository.countGroupByCollegeAndDepartment();

        return rows.stream()
                .collect(Collectors.groupingBy(LabCollegeDeptCountRow::college)) // 각 row를 college를 기준으로 묶음
                .entrySet().stream() // 위에서 만든 Map을 다시 stream으로
                .map(entry -> LabCountByCollegeResponseDto.of(
                        entry.getKey(), // college키
                        entry.getValue().stream() // department값
                                .sorted(Comparator.comparing(LabCollegeDeptCountRow::count).reversed()) // 학과별로 갯수
                                .map(LabCountByDeptResponseDto::from)
                                .toList()
                ))
                .toList();
    }
}
