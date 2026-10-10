package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * 로그인한 계정(memberId)과 연동된 교수를 조회. 연동 안 된 계정이면 404
     */
    public Professor getByMemberId(Long memberId) {
        return professorRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));
    }

    /**
     * 로그인한 계정(memberId)과 연동된 교수를 조회. 연동 안 된 계정이면 비어 있는 Optional (내 정보 조회처럼 없어도 되는 경우용)
     */
    public Optional<Professor> findByMemberId(Long memberId) {
        return professorRepository.findByMemberId(memberId);
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
        Professor professor = getByMemberId(memberId);

        professor.updateProfile(request.positionRaw(), request.phoneNumber(), request.email());

        return ProfessorResponseDto.from(professor);
    }

    /**
     * 온보딩에서 교수 본인이 입력한 학과+이름으로 아직 연동되지 않은 교수 레코드를 찾아 계정과 연결한다.
     * 이름만으로는 동명이인이 있을 수 있어 학과까지 받아 후보를 좁힌다.
     * 후보가 없으면 존재하지 않는 교수, 그래도 여러 명이면(같은 학과 동명이인) 자동 연동 불가로 처리한다
     */
    @Transactional
    public void linkByDepartmentAndName(Member member, Department department, String name) {
        List<Professor> candidates = professorRepository.findAllByDepartmentAndNameAndMemberIsNull(department, name);

        if (candidates.isEmpty()) {
            throw new MyException(ErrorCode.PROFESSOR_NOT_FOUND);
        }
        if (candidates.size() > 1) {
            throw new MyException(ErrorCode.PROFESSOR_NAME_AMBIGUOUS);
        }

        candidates.get(0).linkMember(member);
    }
}
