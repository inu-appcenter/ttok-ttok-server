package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.bookmark.service.BookmarkService;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.SchoolAuthRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.JwtTokenProvider;
import com.inuappcenter.team_2_project_server.domain.member.service.MemberService;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class MemberServiceDeleteTest {

    private MemberRepository memberRepository;
    private ResearcherService researcherService;
    private CoffeeChatService coffeeChatService;
    private LabReviewService labReviewService;
    private ProfessorService professorService;
    private BookmarkService bookmarkService;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        researcherService = mock(ResearcherService.class);
        coffeeChatService = mock(CoffeeChatService.class);
        labReviewService = mock(LabReviewService.class);
        professorService = mock(ProfessorService.class);
        bookmarkService = mock(BookmarkService.class);
        memberService = new MemberService(
                mock(SchoolAuthRepository.class),
                memberRepository,
                mock(JwtTokenProvider.class),
                researcherService,
                mock(LaboratoryService.class),
                coffeeChatService,
                labReviewService,
                professorService,
                bookmarkService
        );
    }

    @Test
    void withdrawal_cleans_up_in_order_before_deleting_member() {
        Member member = Member.create("202400001", "학생", null, null);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.deleteMember(1L);

        // 연구자 정리(커피챗/리뷰 포함)는 ResearcherService가 담당하고, userType과 관계없이 항상 호출한다
        InOrder inOrder = inOrder(bookmarkService, researcherService, professorService, memberRepository);
        inOrder.verify(bookmarkService).deleteAllByMemberId(1L);
        inOrder.verify(researcherService).deleteIfPresent(1L);
        inOrder.verify(professorService).unlinkMember(1L);
        inOrder.verify(memberRepository).delete(member);
        verifyNoInteractions(coffeeChatService, labReviewService);
    }

    @Test
    void fails_when_member_does_not_exist() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verifyNoInteractions(bookmarkService, researcherService, coffeeChatService, labReviewService, professorService);
    }
}
