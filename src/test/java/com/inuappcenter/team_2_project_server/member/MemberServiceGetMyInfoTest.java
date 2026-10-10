package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.bookmark.dto.BookmarkResponseDto;
import com.inuappcenter.team_2_project_server.domain.bookmark.service.BookmarkService;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LaboratoryCapacityDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.SchoolAuthRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.JwtTokenProvider;
import com.inuappcenter.team_2_project_server.domain.member.service.MemberService;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class MemberServiceGetMyInfoTest {

    private MemberRepository memberRepository;
    private ProfessorService professorService;
    private LaboratoryService laboratoryService;
    private BookmarkService bookmarkService;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        professorService = mock(ProfessorService.class);
        laboratoryService = mock(LaboratoryService.class);
        bookmarkService = mock(BookmarkService.class);
        memberService = new MemberService(
                mock(SchoolAuthRepository.class),
                memberRepository,
                mock(JwtTokenProvider.class),
                mock(ResearcherService.class),
                laboratoryService,
                mock(CoffeeChatService.class),
                mock(LabReviewService.class),
                professorService,
                bookmarkService
        );
    }

    @Test
    void getMyInfo_returns_professor_and_laboratory_for_linked_professor() {
        Member member = professorMember();
        Professor professor = professor();
        LaboratoryResponseDto laboratory = laboratoryResponse(professor);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(professorService.findByMemberId(1L)).willReturn(Optional.of(professor));
        given(laboratoryService.findLabByProfessorId(10L)).willReturn(Optional.of(laboratory));

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.professor().id()).isEqualTo(10L);
        assertThat(result.professor().name()).isEqualTo("홍길동");
        assertThat(result.laboratory()).isEqualTo(laboratory);
        assertThat(result.coffeeChat()).isNull();
        assertThat(result.labReview()).isNull();
    }

    @Test
    void getMyInfo_returns_professor_without_laboratory_when_professor_has_no_laboratory() {
        Member member = professorMember();
        Professor professor = professor();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(professorService.findByMemberId(1L)).willReturn(Optional.of(professor));
        given(laboratoryService.findLabByProfessorId(10L)).willReturn(Optional.empty());

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.professor().id()).isEqualTo(10L);
        assertThat(result.laboratory()).isNull();
    }

    @Test
    void getMyInfo_returns_null_professor_when_not_linked_yet() {
        Member member = professorMember();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(professorService.findByMemberId(1L)).willReturn(Optional.empty());

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.professor()).isNull();
        assertThat(result.laboratory()).isNull();
        verify(laboratoryService, never()).findLabByProfessorId(anyLong());
    }

    @Test
    void getMyInfo_does_not_look_up_professor_for_non_professor() {
        Member member = Member.create("202400001", "학생", Department.COMPUTER_ENGINEERING, "student@inu.ac.kr");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.professor()).isNull();
        verify(professorService, never()).findByMemberId(anyLong());
    }

    @Test
    void getMyInfo_includes_my_bookmarks() {
        Member member = Member.create("202400001", "학생", Department.COMPUTER_ENGINEERING, "student@inu.ac.kr");
        BookmarkResponseDto bookmark = new BookmarkResponseDto(1L, laboratoryResponse(professor()));

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(bookmarkService.getMyBookmark(1L)).willReturn(List.of(bookmark));

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.bookmark()).containsExactly(bookmark);
    }

    @Test
    void getMyInfo_returns_empty_bookmarks_when_none() {
        Member member = Member.create("202400001", "학생", Department.COMPUTER_ENGINEERING, "student@inu.ac.kr");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(bookmarkService.getMyBookmark(1L)).willReturn(List.of());

        MemberResponseDto result = memberService.getMyInfo(1L);

        assertThat(result.bookmark()).isEmpty();
    }

    private Member professorMember() {
        Member member = Member.create("12345678", "교수", Department.COMPUTER_ENGINEERING, "hong@inu.ac.kr");
        member.assignUserType(UserType.PROFESSOR);
        return member;
    }

    private Professor professor() {
        Professor professor = Professor.create(
                "홍길동", "교수",
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );
        ReflectionTestUtils.setField(professor, "id", 10L);
        return professor;
    }

    private LaboratoryResponseDto laboratoryResponse(Professor professor) {
        return new LaboratoryResponseDto(
                1L,
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY.getCollegeName(),
                Department.COMPUTER_ENGINEERING,
                Department.COMPUTER_ENGINEERING.getDepartmentName(),
                "AI연구실",
                "7호관 401호",
                new LaboratoryCapacityDto(6, 7),
                null,
                ProfessorResponseDto.from(professor),
                "https://lab.example.com",
                List.of()
        );
    }
}
