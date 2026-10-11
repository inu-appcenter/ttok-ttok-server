package com.inuappcenter.team_2_project_server.domain.member.service;

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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResearcherService {

    private final ResearcherRepository researcherRepository;
    private final MemberRepository memberRepository;
    private final LaboratoryRepository laboratoryRepository;

    /**
     * 연구자 등록
     */
    @Transactional
    public ResearcherResponseDto register(Long memberId, Long laboratoryId, String name) {
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

        Researcher researcher = Researcher.create(member, laboratory, name);

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
     * memberId로 연구자 id 조회. 연구자가 아니면 비어 있는 Optional (회원 탈퇴처럼 없어도 되는 경우용)
     */
    public Optional<Long> findIdByMemberId(Long memberId) {
        return researcherRepository.findByMemberId(memberId)
                .map(Researcher::getId);
    }

    /**
     * 연구자 삭제 (회원 탈퇴용). 이 연구자의 커피챗 삭제, 리뷰 익명화를 먼저 해야 한다
     */
    @Transactional
    public void delete(Long researcherId) {
        researcherRepository.deleteById(researcherId);
    }
}
