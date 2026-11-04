package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResearcherService {

    private final ResearcherRepository researcherRepository;
    private final MemberRepository memberRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final CoffeeChatService coffeeChatService;
    private final LabReviewService labReviewService;

    /**
     * 연구자 등록
     */
    @Transactional
    public ResearcherResponseDto register(Long memberId, Long laboratoryId) {
        if (researcherRepository.existsByMemberId(memberId)) {
            throw new MyException(ErrorCode.RESEARCHER_ALREADY_EXISTS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getUserType() == UserType.PROFESSOR) {
            throw new MyException(ErrorCode.ALREADY_PROFESSOR);
        }

        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        Researcher researcher = Researcher.create(member, laboratory);

        member.assignUserType(UserType.RESEARCHER);

        return ResearcherResponseDto.from(researcherRepository.save(researcher));
    }

    /**
     * memberId 로 연구자 단건 조회
     */
    public ResearcherResponseDto getByMemberId(Long memberId) {
        return researcherRepository.findByMemberId(memberId)
                .map(ResearcherResponseDto::from)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));
    }

    /**
     * 이 멤버가 이 연구실의 연구자(학부연구생/대학원생)로 등록되어 있는지 여부.
     * 담당 교수 본인 여부는 이 서비스가 알 바 아니므로(그건 laboratory 도메인 데이터) 다루지 않는다 — 호출부에서 별도로 확인할 것
     */
    public boolean isAffiliated(Long memberId, Long laboratoryId) {
        return researcherRepository.existsByMemberIdAndLaboratoryId(memberId, laboratoryId);
    }

    /**
     * 본인 연구자 등록 취소 (API용). 회원은 남기고 연구자 레코드만 지운 뒤 유저 타입을 FINDER로 되돌린다. 연구자가 아니면 404
     */
    @Transactional
    public void unregister(Long memberId) {
        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        remove(researcher);
        researcher.getMember().assignUserType(UserType.FINDER);
    }

    /**
     * 연구자 레코드 정리 (회원 탈퇴용). 연구자가 아니면 아무 일도 하지 않는다
     */
    @Transactional
    public void deleteIfPresent(Long memberId) {
        researcherRepository.findByMemberId(memberId)
                .ifPresent(this::remove);
    }

    // 커피챗 삭제 -> 리뷰 익명화(작성자 연결만 끊음) -> 연구자 삭제. 커피챗/리뷰가 연구자를 참조하므로(FK) 연구자를 마지막에 지운다
    private void remove(Researcher researcher) {
        coffeeChatService.deleteByResearcherId(researcher.getId());
        labReviewService.anonymizeByResearcherId(researcher.getId());
        researcherRepository.delete(researcher);
    }
}
