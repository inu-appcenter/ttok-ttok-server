package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProfessorServiceTest {

    private ProfessorRepository professorRepository;
    private MemberRepository memberRepository;
    private ProfessorService professorService;

    @BeforeEach
    void setUp() {
        professorRepository = mock(ProfessorRepository.class);
        memberRepository = mock(MemberRepository.class);
        professorService = new ProfessorService(professorRepository, memberRepository);
    }

    @Test
    void getProfessor_succeeds() {
        Professor professor = professor();
        given(professorRepository.findById(1L)).willReturn(Optional.of(professor));

        Professor result = professorService.getProfessor(1L);

        assertThat(result).isEqualTo(professor);
    }

    @Test
    void getProfessor_fails_when_not_found() {
        given(professorRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.getProfessor(1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
    }

    @Test
    void getByDepartmentAndNameAndEmail_succeeds() {
        Professor professor = professor();
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.of(professor));

        Professor result = professorService.getByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        );

        assertThat(result).isEqualTo(professor);
    }

    @Test
    void getByDepartmentAndNameAndEmail_fails_when_not_found() {
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.getByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        ))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
    }

    @Test
    void createIfNotExists_returns_existing_professor_without_saving() {
        Professor professor = professor();
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.of(professor));

        Professor result = professorService.createIfNotExists(
                "홍길동", "교수", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );

        assertThat(result).isEqualTo(professor);
        verify(professorRepository, never()).save(any(Professor.class));
    }

    @Test
    void createIfNotExists_creates_new_professor_when_not_found() {
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.empty());
        given(professorRepository.save(any(Professor.class))).willAnswer(i -> i.getArgument(0));

        Professor result = professorService.createIfNotExists(
                "홍길동", "교수", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );

        assertThat(result.getName()).isEqualTo("홍길동");
        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    void linkMember_succeeds() {
        Professor professor = professor();
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);

        given(professorRepository.findById(10L)).willReturn(Optional.of(professor));
        given(professorRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        ProfessorResponseDto response = professorService.linkMember(10L, 1L);

        assertThat(response.id()).isEqualTo(professor.getId());
        assertThat(professor.getMember()).isEqualTo(member);
    }

    @Test
    void linkMember_fails_when_professor_not_found() {
        given(professorRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.linkMember(10L, 1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
    }

    @Test
    void linkMember_fails_when_professor_already_linked() {
        Professor professor = professor();
        professor.linkMember(mock(Member.class));
        given(professorRepository.findById(10L)).willReturn(Optional.of(professor));

        assertThatThrownBy(() -> professorService.linkMember(10L, 1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_ALREADY_LINKED);

        verify(memberRepository, never()).findById(any());
    }

    @Test
    void linkMember_fails_when_member_already_linked_to_another_professor() {
        Professor professor = professor();
        given(professorRepository.findById(10L)).willReturn(Optional.of(professor));
        given(professorRepository.existsByMemberId(1L)).willReturn(true);

        assertThatThrownBy(() -> professorService.linkMember(10L, 1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_ALREADY_LINKED_TO_PROFESSOR);

        verify(memberRepository, never()).findById(any());
    }

    @Test
    void linkMember_fails_when_member_not_found() {
        Professor professor = professor();
        given(professorRepository.findById(10L)).willReturn(Optional.of(professor));
        given(professorRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.linkMember(10L, 1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    private Professor professor() {
        return Professor.create(
                "홍길동", "교수", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );
    }
}
