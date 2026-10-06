package com.inuappcenter.team_2_project_server.domain.labReview.service;

import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewOptionsResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;
import com.inuappcenter.team_2_project_server.domain.labReview.repository.LabReviewRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LabReviewService {

    private final LabReviewRepository labReviewRepository;
    private final ResearcherRepository researcherRepository;

    /**
     * 연구실 리뷰 생성
     */
    public LabReviewResponseDto submit(Long memberId, LabReviewRequestDto request) {
        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        if (labReviewRepository.existsByResearcherId(researcher.getId())) {
            throw new MyException(ErrorCode.LAB_REVIEW_ALREADY_EXISTS);
        }

        LabReview review = LabReview.create(
                researcher.getLaboratory(),
                researcher,
                request.coreTime(),
                request.weeklyMeeting(),
                request.doings()
        );

        // 동시 제출로 researcher_id 유니크 제약에 걸리면 도메인 에러로 변환
        try {
            labReviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new MyException(ErrorCode.LAB_REVIEW_ALREADY_EXISTS);
        }

        return LabReviewResponseDto.from(review);
    }

    /**
     * 리뷰 작성 폼 기본 선택지 조회
     */
    @Transactional(readOnly = true)
    public LabReviewOptionsResponseDto getOptions() {
        return LabReviewOptionsResponseDto.defaults();
    }

    /**
     * 연구실 리뷰 조회
     */
    @Transactional(readOnly = true)
    public List<LabReviewResponseDto> getLabReviews(Long laboratoryId) {
        return labReviewRepository.findAllByLaboratoryId(laboratoryId)
                .stream().map(LabReviewResponseDto::from).toList();
    }

    /**
     * 내 연구실 리뷰 조회
     */
    @Transactional(readOnly = true)
    public LabReviewResponseDto getMyLabReview(Long memberId) {
        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        return labReviewRepository.findByResearcherId(researcher.getId())
                .map(LabReviewResponseDto::from)
                .orElse(null);
    }

    /**
     * 내 연구실 리뷰 수정
     */
    public LabReviewResponseDto updateMyLabReview(Long memberId, LabReviewRequestDto request) {
        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        LabReview review = labReviewRepository.findByResearcherId(researcher.getId())
                .orElseThrow(() -> new MyException(ErrorCode.LAB_REVIEW_NOT_FOUND));

        review.update(
                request.coreTime(),
                request.weeklyMeeting(),
                request.doings()
        );

        return LabReviewResponseDto.from(review);
    }

    /**
     * 내 연구실 리뷰 삭제
     */
    public Long deleteMyLabReview(Long memberId) {
        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        LabReview review = labReviewRepository.findByResearcherId(researcher.getId())
                .orElseThrow(() -> new MyException(ErrorCode.LAB_REVIEW_NOT_FOUND));

        Long deletedReviewId = review.getId();

        labReviewRepository.delete(review);

        return deletedReviewId;
    }
}
