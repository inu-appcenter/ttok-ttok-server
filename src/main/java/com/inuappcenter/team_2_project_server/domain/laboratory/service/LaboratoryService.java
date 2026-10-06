package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCapacityUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LabCountByCollegeResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LabCountByDeptResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
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

import java.util.Arrays;
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
    // 카테고리/단과대/학과로 범위를 좁히고, 그 안에서 키워드로 검색한다
    // 단과대/학과는 한글 이름(예: 공과대학, 전자공학부)으로 받아 enum으로 변환 (없는 이름이면 INVALID_INPUT)
    // 키워드는 공백을 무시하고 연구실명/교수명/학과명/세부 연구분야명/카테고리명과 부분일치로 비교
    @Transactional(readOnly = true)
    public Page<LaboratoryResponseDto> searchLabs(
            String keyword,
            List<String> categoryNames,
            String collegeName,
            String departmentName,
            Pageable pageable
    ) {
        List<String> categories = normalizeCategories(categoryNames);
        College college = isBlank(collegeName) ? null : College.fromCollegeName(collegeName.trim());
        Department department = isBlank(departmentName) ? null : Department.fromDepartmentName(departmentName.trim());
        String normalizedKeyword = normalizeKeyword(keyword);

        return laboratoryRepository.searchByFilter(
                categories.isEmpty(),
                categories,
                college,
                department,
                escapeLike(normalizedKeyword),
                findDepartmentsContaining(normalizedKeyword),
                pageable
        ).map(LaboratoryResponseDto::from);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 카테고리는 각 값을 trim하고, 빈 값과 중복은 제거
    private List<String> normalizeCategories(List<String> categoryNames) {
        if (categoryNames == null) {
            return List.of();
        }

        return categoryNames.stream()
                .filter(name -> !isBlank(name))
                .map(String::trim)
                .distinct()
                .toList();
    }

    // 공백을 무시하고 비교하기 위해 모든 공백을 제거하고 소문자로 변환 (null이면 빈 문자열 = 전체)
    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : removeWhitespace(keyword).toLowerCase();
    }

    private String removeWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    // 학과는 enum이라 DB에서 이름으로 비교할 수 없으므로, 한글 학과명이 키워드를 포함하는 학과를 미리 찾아 넘김
    private List<Department> findDepartmentsContaining(String normalizedKeyword) {
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(Department.values())
                .filter(department -> removeWhitespace(department.getDepartmentName()).toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    // like 검색에서 %, _ 가 와일드카드로 동작하지 않도록 이스케이프 (escape 문자는 \)
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
