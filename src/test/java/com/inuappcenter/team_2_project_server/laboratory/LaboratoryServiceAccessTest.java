package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 연구실 수정/삭제는 담당 교수 본인과 관리자만 할 수 있다 (#92)
 * 소속 연구자는 온보딩에서 검증 없이 등록되므로 권한이 없다
 */
class LaboratoryServiceAccessTest {

    private static final Long LAB_ID = 10L;
    private static final Long MEMBER_ID = 1L;

    private LaboratoryRepository laboratoryRepository;
    private LaboratoryService laboratoryService;
    private Laboratory laboratory;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        laboratoryService = new LaboratoryService(laboratoryRepository, mock(ProfessorService.class));
        laboratory = laboratory();
        given(laboratoryRepository.findById(LAB_ID)).willReturn(Optional.of(laboratory));
    }

    @Test
    void update_fails_when_member_is_not_owner_professor_or_admin() {
        // 소속 연구자를 포함해, 담당 교수도 관리자도 아닌 회원
        Member member = member("ROLE_USER");
        given(laboratoryRepository.existsByIdAndProfessor_MemberId(LAB_ID, MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> laboratoryService.updateLab(LAB_ID, updateRequest(), member))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LAB_ACCESS);
        assertThat(laboratory.getLabName()).isEqualTo("AI연구실");
    }

    @Test
    void delete_fails_when_member_is_not_owner_professor_or_admin() {
        Member member = member("ROLE_USER");
        given(laboratoryRepository.existsByIdAndProfessor_MemberId(LAB_ID, MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> laboratoryService.deleteLab(member, LAB_ID))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LAB_ACCESS);
        verify(laboratoryRepository, never()).delete(any());
    }

    @Test
    void update_succeeds_for_owner_professor() {
        Member member = member("ROLE_USER");
        given(laboratoryRepository.existsByIdAndProfessor_MemberId(LAB_ID, MEMBER_ID)).willReturn(true);

        LaboratoryResponseDto response = laboratoryService.updateLab(LAB_ID, updateRequest(), member);

        assertThat(response.labName()).isEqualTo("새 연구실명");
    }

    @Test
    void delete_succeeds_for_owner_professor() {
        Member member = member("ROLE_USER");
        given(laboratoryRepository.existsByIdAndProfessor_MemberId(LAB_ID, MEMBER_ID)).willReturn(true);

        laboratoryService.deleteLab(member, LAB_ID);

        verify(laboratoryRepository).delete(laboratory);
    }

    @Test
    void update_succeeds_for_admin_without_owner_check() {
        Member admin = member("ROLE_ADMIN");

        LaboratoryResponseDto response = laboratoryService.updateLab(LAB_ID, updateRequest(), admin);

        assertThat(response.labName()).isEqualTo("새 연구실명");
        verify(laboratoryRepository, never()).existsByIdAndProfessor_MemberId(anyLong(), anyLong());
    }

    @Test
    void delete_succeeds_for_admin_without_owner_check() {
        Member admin = member("ROLE_ADMIN");

        laboratoryService.deleteLab(admin, LAB_ID);

        verify(laboratoryRepository).delete(laboratory);
        verify(laboratoryRepository, never()).existsByIdAndProfessor_MemberId(anyLong(), anyLong());
    }

    private Member member(String role) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(MEMBER_ID);
        given(member.getRole()).willReturn(role);
        return member;
    }

    private LaboratoryUpdateRequestDto updateRequest() {
        return new LaboratoryUpdateRequestDto("새 연구실명", null, null, null, null, null);
    }

    private Laboratory laboratory() {
        Professor professor = Professor.create(
                "홍길동", "교수",
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );
        return Laboratory.create(
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING,
                "AI연구실",
                "7호관 401호",
                6,
                7,
                null,
                professor,
                "https://lab.example.com",
                null
        );
    }
}
