package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final MemberRepository memberRepository;

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
        return professorRepository.findByDepartmentAndNameAndEmail(department, name, email)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));
    }

    /**
     * 엑셀 임포트용: 이미 있으면 그대로, 없으면 새로 생성
     */
    @Transactional
    public Professor createIfNotExists(
            String name,
            String positionRaw,
            College college,
            Department department,
            String phoneNumber,
            String email
    ) {
        return professorRepository.findByDepartmentAndNameAndEmail(department, name, email)
                .orElseGet(() -> professorRepository.save(
                        Professor.create(name, positionRaw, college, department, phoneNumber, email)
                ));
    }

    /**
     * 관리자가 본인 확인을 마친 교수 계정을 기존 Professor 레코드와 연동
     */
    @Transactional
    public ProfessorResponseDto linkMember(Long professorId, Long memberId) {
        Professor professor = getProfessor(professorId);

        if (professor.getMember() != null) {
            throw new MyException(ErrorCode.PROFESSOR_ALREADY_LINKED);
        }

        if (professorRepository.existsByMemberId(memberId)) {
            throw new MyException(ErrorCode.MEMBER_ALREADY_LINKED_TO_PROFESSOR);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        professor.linkMember(member);

        return ProfessorResponseDto.from(professor);
    }
}
