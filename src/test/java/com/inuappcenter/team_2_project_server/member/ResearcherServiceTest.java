package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
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

class ResearcherServiceTest {

    private ResearcherRepository researcherRepository;
    private MemberRepository memberRepository;
    private LaboratoryRepository laboratoryRepository;
    private ResearcherService researcherService;

    @BeforeEach
    void setUp() {
        researcherRepository = mock(ResearcherRepository.class);
        memberRepository = mock(MemberRepository.class);
        laboratoryRepository = mock(LaboratoryRepository.class);
        researcherService = new ResearcherService(researcherRepository, memberRepository, laboratoryRepository);
    }

    @Test
    void register_succeeds_and_maps_response() {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getStudentNumber()).willReturn("20240001");
        Laboratory laboratory = mock(Laboratory.class);
        given(laboratory.getId()).willReturn(10L);
        given(laboratory.getLabName()).willReturn("소프트웨어공학 연구실");

        given(researcherRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(laboratoryRepository.findById(10L)).willReturn(Optional.of(laboratory));
        given(researcherRepository.save(any(Researcher.class))).willAnswer(i -> i.getArgument(0));

        ResearcherResponseDto response = researcherService.register(1L, 10L, "홍길동");

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.studentNumber()).isEqualTo("20240001");
        assertThat(response.laboratoryId()).isEqualTo(10L);
        assertThat(response.laboratoryName()).isEqualTo("소프트웨어공학 연구실");
        assertThat(response.validateYN()).isFalse();
        verify(researcherRepository).save(any(Researcher.class));
        // 등록 API로 연구자가 되어도 유저 타입이 연구자 레코드와 맞도록 RESEARCHER로 바꾼다
        verify(member).assignUserType(UserType.RESEARCHER);
    }

    @Test
    void register_fails_when_member_is_professor() {
        Member professorMember = mock(Member.class);
        given(professorMember.getUserType()).willReturn(UserType.PROFESSOR);
        given(researcherRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(professorMember));

        assertThatThrownBy(() -> researcherService.register(1L, 10L, "홍길동"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_PROFESSOR);

        verify(professorMember, never()).assignUserType(any());
        verify(researcherRepository, never()).save(any());
    }

    @Test
    void register_fails_when_already_registered() {
        given(researcherRepository.existsByMemberId(1L)).willReturn(true);

        assertThatThrownBy(() -> researcherService.register(1L, 10L, "홍길동"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESEARCHER_ALREADY_EXISTS);

        verify(memberRepository, never()).findById(any());
        verify(researcherRepository, never()).save(any());
    }

    @Test
    void register_fails_when_member_not_found() {
        given(researcherRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> researcherService.register(1L, 10L, "홍길동"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verify(researcherRepository, never()).save(any());
    }

    @Test
    void register_fails_when_laboratory_not_found() {
        given(researcherRepository.existsByMemberId(1L)).willReturn(false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(mock(Member.class)));
        given(laboratoryRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> researcherService.register(1L, 10L, "홍길동"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABORATORY_NOT_FOUND);

        verify(researcherRepository, never()).save(any());
    }

    @Test
    void getByMemberId_succeeds() {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getStudentNumber()).willReturn("20240001");
        Laboratory laboratory = mock(Laboratory.class);
        given(laboratory.getId()).willReturn(10L);
        given(laboratory.getLabName()).willReturn("소프트웨어공학 연구실");
        Researcher researcher = Researcher.create(member, laboratory, "홍길동");

        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));

        ResearcherResponseDto response = researcherService.getByMemberId(1L);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.laboratoryName()).isEqualTo("소프트웨어공학 연구실");
    }

    @Test
    void getByMemberId_fails_when_not_found() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> researcherService.getByMemberId(1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESEARCHER_NOT_FOUND);
    }

    @Test
    void researcher_create_rejects_null_laboratory() {
        assertThatThrownBy(() -> Researcher.create(mock(Member.class), null, "홍길동"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABORATORY_NOT_FOUND);
    }
}
