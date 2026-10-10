package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class ProfessorServiceTest {

    private ProfessorRepository professorRepository;
    private ProfessorService professorService;

    @BeforeEach
    void setUp() {
        professorRepository = mock(ProfessorRepository.class);
        professorService = new ProfessorService(professorRepository);
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
    void findByMemberId_returns_linked_professor() {
        Professor professor = professor();
        given(professorRepository.findByMemberId(1L)).willReturn(Optional.of(professor));

        Optional<Professor> result = professorService.findByMemberId(1L);

        assertThat(result).contains(professor);
    }

    @Test
    void findByMemberId_returns_empty_when_not_linked() {
        given(professorRepository.findByMemberId(1L)).willReturn(Optional.empty());

        Optional<Professor> result = professorService.findByMemberId(1L);

        assertThat(result).isEmpty();
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
    void getByDepartmentAndNameAndEmail_finds_linked_professor_even_when_email_drifted() {
        // 연동된 교수가 스스로 이메일을 고쳐서 엑셀 원본 이메일과 달라진 상황을 가정
        Professor professor = professor();
        given(professorRepository.findByDepartmentAndNameAndMemberIsNotNull(
                Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(Optional.of(professor));

        Professor result = professorService.getByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "old-excel-value@inu.ac.kr"
        );

        assertThat(result).isEqualTo(professor);
        verify(professorRepository, never()).findByDepartmentAndNameAndEmail(any(), any(), any());
    }

    @Test
    void upsertFromExcel_creates_new_professor_when_not_found() {
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.empty());
        given(professorRepository.save(any(Professor.class))).willAnswer(i -> i.getArgument(0));

        Professor result = professorService.upsertFromExcel(
                "홍길동", "교수",
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );

        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getCollege()).isEqualTo(College.COLLEGE_OF_INFORMATION_TECHNOLOGY);
        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    void upsertFromExcel_updates_existing_unlinked_professor() {
        Professor professor = professor();
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING, "홍길동", "hong@inu.ac.kr"
        )).willReturn(Optional.of(professor));

        Professor result = professorService.upsertFromExcel(
                "홍길동", "부교수",
                Department.COMPUTER_ENGINEERING, "032-111-1111", "hong@inu.ac.kr"
        );

        assertThat(result.getPositionRaw()).isEqualTo("부교수");
        assertThat(result.getPhoneNumber()).isEqualTo("032-111-1111");
        verify(professorRepository, never()).save(any(Professor.class));
    }

    @Test
    void upsertFromExcel_does_not_update_already_linked_professor() {
        Professor professor = professor();
        ReflectionTestUtils.setField(professor, "member", mock(Member.class));
        given(professorRepository.findByDepartmentAndNameAndMemberIsNotNull(
                Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(Optional.of(professor));

        Professor result = professorService.upsertFromExcel(
                "홍길동", "부교수",
                Department.COMPUTER_ENGINEERING, "032-111-1111", "hong@inu.ac.kr"
        );

        assertThat(result.getPositionRaw()).isEqualTo("교수");
        assertThat(result.getPhoneNumber()).isEqualTo("032-000-0000");
        verify(professorRepository, never()).save(any(Professor.class));
    }

    @Test
    void updateMyProfile_changes_only_requested_fields() {
        Professor professor = professor();
        given(professorRepository.findByMemberId(1L)).willReturn(Optional.of(professor));

        professorService.updateMyProfile(1L, new ProfessorUpdateRequestDto("부교수", null, null));

        assertThat(professor.getPositionRaw()).isEqualTo("부교수");
        assertThat(professor.getPhoneNumber()).isEqualTo("032-000-0000");
    }

    @Test
    void updateMyProfile_changes_email_when_provided() {
        Professor professor = professor();
        given(professorRepository.findByMemberId(1L)).willReturn(Optional.of(professor));

        professorService.updateMyProfile(1L, new ProfessorUpdateRequestDto(null, null, "new@inu.ac.kr"));

        assertThat(professor.getEmail()).isEqualTo("new@inu.ac.kr");
    }

    @Test
    void updateMyProfile_fails_when_not_linked_to_any_professor() {
        given(professorRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> professorService.updateMyProfile(1L, new ProfessorUpdateRequestDto("부교수", null, null)))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
    }

    @Test
    void linkByDepartmentAndName_links_the_single_unlinked_match() {
        Professor professor = professor();
        Member member = mock(Member.class);
        given(professorRepository.findAllByDepartmentAndNameAndMemberIsNull(
                Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of(professor));

        professorService.linkByDepartmentAndName(member, Department.COMPUTER_ENGINEERING, "홍길동");

        assertThat(professor.getMember()).isEqualTo(member);
    }

    @Test
    void linkByDepartmentAndName_fails_when_no_candidate() {
        given(professorRepository.findAllByDepartmentAndNameAndMemberIsNull(
                Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of());

        assertThatThrownBy(() -> professorService.linkByDepartmentAndName(
                mock(Member.class), Department.COMPUTER_ENGINEERING, "홍길동"
        ))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
    }

    @Test
    void linkByDepartmentAndName_fails_when_multiple_candidates_in_same_department() {
        given(professorRepository.findAllByDepartmentAndNameAndMemberIsNull(
                Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of(professor(), professor()));

        assertThatThrownBy(() -> professorService.linkByDepartmentAndName(
                mock(Member.class), Department.COMPUTER_ENGINEERING, "홍길동"
        ))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFESSOR_NAME_AMBIGUOUS);
    }

    private Professor professor() {
        return Professor.create(
                "홍길동", "교수",
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );
    }
}
