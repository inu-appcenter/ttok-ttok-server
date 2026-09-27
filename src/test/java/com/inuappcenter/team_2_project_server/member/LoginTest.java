package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.member.dto.LocalAuthLoginDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.LoginRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.LoginResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.SchoolAuthRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.JwtTokenProvider;
import com.inuappcenter.team_2_project_server.domain.member.service.MemberService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class LoginTest {

    private SchoolAuthRepository schoolAuthRepository;
    private MemberRepository memberRepository;
    private JwtTokenProvider jwtTokenProvider;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        schoolAuthRepository = mock(SchoolAuthRepository.class);
        memberRepository = mock(MemberRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        memberService = new MemberService(schoolAuthRepository, memberRepository, jwtTokenProvider);
    }

    @Test
    void login_succeeds_with_existing_member() {
        LoginRequestDto request = new LoginRequestDto("20240001", "password");
        Member member = Member.create("20240001", null, null, null);

        given(schoolAuthRepository.authenticate("20240001", "password"))
                .willReturn(Optional.of(new LocalAuthLoginDto("20240001", "ROLE_USER")));
        given(memberRepository.findByStudentNumber("20240001")).willReturn(Optional.of(member));
        given(jwtTokenProvider.createAccessToken(member)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(member)).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpiresAt()).willReturn(LocalDateTime.of(2026, 8, 11, 18, 0));
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.of(2026, 8, 25, 17, 0));

        LoginResponseDto response = memberService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.accessTokenExpiresAt()).isEqualTo("2026-08-11T18:00");
        assertThat(response.refreshTokenExpiresAt()).isEqualTo("2026-08-25T17:00");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void login_creates_member_when_school_auth_succeeds_and_member_does_not_exist() {
        LoginRequestDto request = new LoginRequestDto("20240002", "password");
        Member savedMember = Member.createWithRole("20240002", null, null, null, "ROLE_ADMIN");

        given(schoolAuthRepository.authenticate("20240002", "password"))
                .willReturn(Optional.of(new LocalAuthLoginDto("20240002", "ROLE_ADMIN")));
        given(memberRepository.findByStudentNumber("20240002")).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);
        given(jwtTokenProvider.createAccessToken(savedMember)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(savedMember)).willReturn("new-refresh-token");
        given(jwtTokenProvider.getAccessTokenExpiresAt()).willReturn(LocalDateTime.of(2026, 8, 11, 18, 0));
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.of(2026, 8, 25, 17, 0));

        LoginResponseDto response = memberService.login(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(memberRepository).save(argThat(member -> "ROLE_ADMIN".equals(member.getRole())));
    }

    @Test
    void login_fails_when_school_auth_fails() {
        LoginRequestDto request = new LoginRequestDto("20240003", "wrong-password");

        given(schoolAuthRepository.authenticate("20240003", "wrong-password")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(memberRepository, never()).findByStudentNumber(any());
        verify(memberRepository, never()).save(any(Member.class));
        verify(jwtTokenProvider, never()).createAccessToken(any(Member.class));
        verify(jwtTokenProvider, never()).createRefreshToken(any(Member.class));
    }
}
