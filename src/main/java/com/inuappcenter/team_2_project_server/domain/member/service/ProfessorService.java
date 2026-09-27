package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    /**
     * 교수 ID로 단건 조회
     */
    public Professor getProfessor(Long professorId) {
        return professorRepository.findById(professorId)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));
    }

    /**
     * 단과대·학과·이름·이메일로 교수 조회
     */
    public Professor getByDepartmentAndNameAndEmail(Department department, String name, String email) {
        return resolveByExcelIdentity(department, name, email)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));
    }

    /**
     * 엑셀 임포트용: 이미 있으면 계정과 연동되지 않은 경우에만 최신 값으로 갱신하고, 없으면 새로 생성.
     * 이미 계정과 연동된 교수는 본인이 직접 고쳤을 수 있으므로 엑셀 값으로 덮어쓰지 않는다
     */
    @Transactional
    public Professor upsertFromExcel(
            String name,
            String positionRaw,
            Department department,
            String phoneNumber,
            String email
    ) {
        return resolveByExcelIdentity(department, name, email)
                .map(existing -> {
                    if (existing.getMember() == null) {
                        existing.updateFromExcel(positionRaw, phoneNumber);
                    }
                    return existing;
                })
                .orElseGet(() -> professorRepository.save(
                        Professor.create(name, positionRaw, department, phoneNumber, email)
                ));
    }

    /**
     * 엑셀 행이 가리키는 교수를 찾는다. 계정과 연동된 교수는 본인이 이메일을 직접 고쳤을 수 있어
     * 엑셀 원본 이메일과 어긋날 수 있으므로, 이름+학과로 먼저 찾고 없으면 기존처럼 이메일까지 포함해 찾는다
     */
    private Optional<Professor> resolveByExcelIdentity(Department department, String name, String email) {
        return professorRepository.findByDepartmentAndNameAndMemberIsNotNull(department, name)
                .or(() -> professorRepository.findByDepartmentAndNameAndEmail(department, name, email));
    }

    /**
     * 교수 본인이 자기 정보를 직접 수정. memberId로 연동된 Professor를 찾기 때문에
     * 본인이 아닌 다른 교수 레코드는 애초에 조회조차 되지 않는다 (연동 안 된 경우 404)
     */
    @Transactional
    public ProfessorResponseDto updateMyProfile(
            Long memberId,
            ProfessorUpdateRequestDto request
    ) {
        Professor professor = professorRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));

        professor.updateProfile(request.positionRaw(), request.phoneNumber(), request.email());

        return ProfessorResponseDto.from(professor);
    }
}
