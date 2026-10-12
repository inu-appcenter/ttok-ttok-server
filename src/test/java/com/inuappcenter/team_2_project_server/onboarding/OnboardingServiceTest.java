package com.inuappcenter.team_2_project_server.onboarding;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.enums.ContactType;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.domain.onboarding.dto.OnboardingRequestDto;
import com.inuappcenter.team_2_project_server.domain.onboarding.service.OnboardingService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {

    private MemberRepository memberRepository;
    private ResearcherService researcherService;
    private ProfessorService professorService;
    private LabReviewService labReviewService;
    private CoffeeChatService coffeeChatService;
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        researcherService = mock(ResearcherService.class);
        professorService = mock(ProfessorService.class);
        labReviewService = mock(LabReviewService.class);
        coffeeChatService = mock(CoffeeChatService.class);
        onboardingService = new OnboardingService(
                memberRepository, researcherService, professorService, labReviewService, coffeeChatService
        );
    }

    @Test
    void professor_links_professor_record_and_keeps_professor_type() {
        Member member = member("12345678");
        member.assignUserType(UserType.PROFESSOR);   // 로그인 시점에 교번으로 이미 정해짐

        MemberResponseDto response = onboardingService.complete(1L, new OnboardingRequestDto(
                UserType.PROFESSOR, Department.CHEMISTRY, "홍교수",
                null, null, null, null, null, null, null, null
        ));

        verify(professorService).linkByDepartmentAndName(member, Department.CHEMISTRY, "홍교수");
        assertThat(response.userType()).isEqualTo(UserType.PROFESSOR);
        assertThat(response.department()).isEqualTo(Department.CHEMISTRY);
        assertThat(response.isNew()).isFalse();
        verifyNoInteractions(researcherService, labReviewService, coffeeChatService);
    }

    @Test
    void researcher_sets_type_and_department_and_registers_with_review_and_coffee_chat() {
        Member member = member("202400001");

        MemberResponseDto response = onboardingService.complete(1L, researcherRequest(true));

        assertThat(response.userType()).isEqualTo(UserType.RESEARCHER);
        assertThat(response.department()).isEqualTo(Department.COMPUTER_ENGINEERING);
        assertThat(response.isNew()).isFalse();
        verify(researcherService).register(1L, 465L, null);
        verify(labReviewService).submit(eq(1L), any(LabReviewRequestDto.class));
        verify(coffeeChatService).createCoffeeChat(eq(1L), any(CoffeeChatCreateRequestDto.class));
        verifyNoInteractions(professorService);
    }

    @Test
    void researcher_without_coffee_chat_does_not_create_coffee_chat() {
        member("202400001");

        onboardingService.complete(1L, researcherRequest(false));

        verify(researcherService).register(1L, 465L, null);
        verify(coffeeChatService, never()).createCoffeeChat(anyLong(), any());
    }

    @Test
    void finder_sets_type_and_department_only() {
        member("202400001");

        MemberResponseDto response = onboardingService.complete(1L, new OnboardingRequestDto(
                UserType.FINDER, null, null,
                null, null, null, null, false, null, null,
                Department.KOREAN
        ));

        assertThat(response.userType()).isEqualTo(UserType.FINDER);
        assertThat(response.department()).isEqualTo(Department.KOREAN);
        assertThat(response.isNew()).isFalse();
        verifyNoInteractions(researcherService, labReviewService, coffeeChatService, professorService);
    }

    @Test
    void student_cannot_choose_professor() {
        Member member = member("202400001");

        assertThatThrownBy(() -> onboardingService.complete(1L, new OnboardingRequestDto(
                UserType.PROFESSOR, Department.CHEMISTRY, "홍교수",
                null, null, null, null, null, null, null, null
        )))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        // 교수 유형은 교번 로그인으로만 정해지므로 교수 레코드 연동도 하지 않는다
        verifyNoInteractions(professorService);
        assertThat(member.isNew()).isTrue();
    }

    @Test
    void fails_when_onboarding_already_done() {
        Member member = member("202400001");
        member.updateIsNew();

        assertThatThrownBy(() -> onboardingService.complete(1L, researcherRequest(false)))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_ALREADY_DONE);
        verifyNoInteractions(researcherService);
    }

    @Test
    void fails_when_member_not_found() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.complete(1L, researcherRequest(false)))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    private Member member(String studentNumber) {
        Member member = Member.create(studentNumber, studentNumber, null, null);
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        return member;
    }

    private OnboardingRequestDto researcherRequest(boolean coffeeChatAllowed) {
        return new OnboardingRequestDto(
                UserType.RESEARCHER, null, null,
                465L, "있음", "주 1회", Set.of("논문 리딩"),
                coffeeChatAllowed,
                coffeeChatAllowed ? ContactType.KAKAO_TALK : null,
                coffeeChatAllowed ? "https://open.kakao.com/o/abcd1234" : null,
                Department.COMPUTER_ENGINEERING
        );
    }
}
