package com.inuappcenter.team_2_project_server.domain.member.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
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

        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        Researcher researcher = Researcher.create(member, laboratory, name);

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
     * 연구실 소속 검증
     */
    public void validateAffiliation(Member member, Long laboratoryId) {
        if (!researcherRepository.existsByMemberIdAndLaboratoryId(member.getId(), laboratoryId)) {
            throw new MyException(ErrorCode.INVALID_LAB_ACCESS);
        }
    }
}
