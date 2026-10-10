package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.bookmark.dto.BookmarkResponseDto;
import com.inuappcenter.team_2_project_server.domain.bookmark.service.BookmarkService;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatResponseDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.dto.LocalAuthLoginDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.LoginRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.MemberCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.MemberUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.TokenReissueRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.LoginResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.SchoolAuthRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final int PROFESSOR_ID_LENGTH = 8;

    private final SchoolAuthRepository schoolAuthRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResearcherService researcherService;
    private final LaboratoryService laboratoryService;
    private final CoffeeChatService coffeeChatService;
    private final LabReviewService labReviewService;
    private final ProfessorService professorService;
    private final BookmarkService bookmarkService;

    /**
     * 로그인 메서드
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String studentNumber = request.studentNumber();
        String password = request.password();

        LocalAuthLoginDto authResult = schoolAuthRepository.authenticate(studentNumber, password)
                .orElseThrow(() -> new MyException(ErrorCode.INVALID_CREDENTIALS));

        Member member = memberRepository.findByStudentNumber(studentNumber)
                .orElseGet(() -> {
                    Member newMember = Member.createWithRole(
                            authResult.studentNumber(), // 학번
                            authResult.studentNumber(), // 닉네임
                            null,                       // 학과
                            null,                       // 단과대
                            authResult.role()           // Security Role
                    );

                    // 학교 계정 체계상 교번은 8자리, 학번은 9자리라 자릿수만으로 교수 여부를 구분할 수 있다
                    if (studentNumber.length() == PROFESSOR_ID_LENGTH) {
                        newMember.assignUserType(UserType.PROFESSOR);
                    }

                    return memberRepository.save(newMember);
                });

        member.recordLogin();

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        // isNew 는 온보딩 미완료 여부. 온보딩(POST /api/onboarding)을 마치면 false 로 내려간다
        return new LoginResponseDto(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiresAt().toString(),
                jwtTokenProvider.getRefreshTokenExpiresAt().toString(),
                member.getId(),
                member.isNew()
        );
    }

    /**
     * 유저 전체 조회
     */
    @Transactional(readOnly = true)
    public List<MemberResponseDto> getMemberAll() {
        List<MemberResponseDto> members = memberRepository.findAll()
                .stream()
                .map(MemberResponseDto::from)
                .toList();

        return members;
    }

    /**
     * 유저 단일 조회
     */
    @Transactional(readOnly = true)
    public MemberResponseDto getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponseDto.from(member);
    }


    /**
     * 유저 프로필 변경 메서드
     */
    @Transactional
    public MemberResponseDto updateMemberProfile(Long memberId, MemberUpdateRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        member.updateMemberProfile(
                request.nickName(),
                request.department(),
                request.email()
        );

        return MemberResponseDto.from(member);
    }

    /**
     * 유저 생성
     */
    @Transactional
    public MemberResponseDto createMember(MemberCreateRequestDto request) {
        if (memberRepository.existsByStudentNumber(request.studentNumber())) {
            throw new MyException(ErrorCode.INVALID_INPUT);
        }


        Member member = Member.create(
                request.studentNumber(),
                request.nickName(),
                request.department(),
                request.email()
        );

        Member savedMember = memberRepository.save(member);

        return MemberResponseDto.from(savedMember);
    }


    /**
     * 유저 삭제
     */
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository.delete(member);
    }

    /**
     * accessToken 재발급
     * refreshToken 을 검증하고, 새 accessToken 과 refreshToken 을 함께 재발급한다. (refresh 회전)
     * 서버에 토큰 저장소가 없어(stateless) 기존 refreshToken 은 만료 전까지는 여전히 유효하다.
     */
    public LoginResponseDto reissue(TokenReissueRequestDto request) {
        String refreshToken = request.refreshToken();

        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        // 로그아웃 후의 refreshToken 재사용 차단
        jwtTokenProvider.validateTokenNotRevoked(refreshToken, member.getTokenInvalidBefore());

        String newAccessToken = jwtTokenProvider.createAccessToken(member);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member);

        return new LoginResponseDto(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiresAt().toString(),
                jwtTokenProvider.getRefreshTokenExpiresAt().toString(),
                member.getId(),
                member.isNew()
        );
    }

    /**
     * 로그아웃
     * 회원의 tokenInvalidBefore 를 현재 시각으로 갱신하여, 지금까지 발급된
     * access/refresh 토큰을 즉시 무효화한다. (전 기기 로그아웃)
     */
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        member.logout();
    }

    /**
     * 내 정보 조회 메서드
     */
    @Transactional(readOnly = true)
    public MemberResponseDto getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        LaboratoryResponseDto laboratory = null;
        CoffeeChatResponseDto coffeeChat = null;
        LabReviewResponseDto labReview = null;
        ProfessorResponseDto professor = null;
        List<BookmarkResponseDto> bookmarks;

        // 연구자일때
        if (member.getUserType() == UserType.RESEARCHER) {
            ResearcherResponseDto researcher = researcherService.getByMemberId(memberId);
            laboratory = laboratoryService.getLab(researcher.laboratoryId());
            coffeeChat = coffeeChatService.getMyCoffeeChat(memberId);
            labReview = labReviewService.getMyLabReview(memberId);
        }

        // 교수일때
        if (member.getUserType() == UserType.PROFESSOR) {
            Professor linkedProfessor = professorService.findByMemberId(memberId).orElse(null);

            if (linkedProfessor != null) {
                professor = ProfessorResponseDto.from(linkedProfessor);
                laboratory = laboratoryService.findLabByProfessorId(linkedProfessor.getId()).orElse(null);
            }
        }

        bookmarks = bookmarkService.getMyBookmark(memberId);

        return MemberResponseDto.of(member, laboratory, coffeeChat, labReview, professor, bookmarks);
    }
}
