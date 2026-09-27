package com.inuappcenter.team_2_project_server.domain.onboarding.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.CoffeeChatCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.domain.onboarding.dto.OnboardingRequestDto;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 흐름을 조합하는 계층.
 * 실제 저장은 각 도메인 서비스(연구자 등록 / 연구실 리뷰 / 커피챗)에 위임하고,
 * 하나의 트랜잭션으로 묶어 중간 실패 시 전체 롤백되게 한다.
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final MemberRepository memberRepository;
    private final ResearcherService researcherService;
    private final LabReviewService labReviewService;
    private final CoffeeChatService coffeeChatService;

    @Transactional
    public MemberResponseDto complete(Long memberId, OnboardingRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        // 온보딩을 완료한 사용자의 경우
        if (!member.isNew()) {
            throw new MyException(ErrorCode.ONBOARDING_ALREADY_DONE);
        }

        // 교수는 로그인 시점에 이미 PROFESSOR로 분류되어 있으므로 학생용 질문(purpose)을 보지 않고 바로 온보딩을 완료한다.
        if (member.getUserType() == UserType.PROFESSOR) {
            member.updateIsNew();
            return MemberResponseDto.from(member);
        }

        member.assignUserType(request.purpose());

        // 학부연구생이라면 아래 로직을 거침
        if (request.purpose() == UserType.RESEARCHER) {
            // 연구자 등록 (온보딩에서는 실명을 받지 않으므로 name 은 null)
            researcherService.register(memberId, request.laboratoryId(), null);

            // 연구실 리뷰 작성
            labReviewService.submit(memberId, new LabReviewRequestDto(
                    request.coreTime(),
                    request.weeklyMeeting(),
                    request.doings()
            ));

            // 커피챗 작성
            if (Boolean.TRUE.equals(request.coffeeChatAllowed())) {
                coffeeChatService.createCoffeeChat(memberId, new CoffeeChatCreateRequestDto(
                        request.laboratoryId(),
                        request.contactType(),
                        request.contactValue()
                ));
            }
        }

        // FINDER는 바로 온보딩 완료 처리
        member.updateIsNew();

        return MemberResponseDto.from(member);
    }
}
