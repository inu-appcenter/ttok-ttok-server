package com.inuappcenter.team_2_project_server.domain.labReview.dto;

import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;

import java.util.List;

/**
 * 연구실 리뷰 작성 폼에 표시할 기본 선택지 목록.
 * 연구생은 이 목록에 없는 값도 자유 입력할 수 있다.
 */
public record LabReviewOptionsResponseDto(
        List<String> coreTime,
        List<String> weeklyMeeting,
        List<String> works
) {
    public static LabReviewOptionsResponseDto defaults() {
        return new LabReviewOptionsResponseDto(
                LabReview.CORE_TIME,
                LabReview.WEEKLY_MEETING,
                LabReview.WORKS
        );
    }
}
