package com.inuappcenter.team_2_project_server.domain.labReview.controller;

import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewOptionsResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lab-review")
public class LabReviewController implements LabReviewApiSpecification {

    private final LabReviewService labReviewService;


    @GetMapping("/options")
    public ResponseEntity<ResponseDto<LabReviewOptionsResponseDto>> getOptions() {
        LabReviewOptionsResponseDto options = labReviewService.getOptions();

        return ResponseEntity.ok(
                ResponseDto.of(options, "연구실 리뷰 선택지 조회 성공")
        );
    }

    @PostMapping
    public ResponseEntity<ResponseDto<LabReviewResponseDto>> submit(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody LabReviewRequestDto request
    ) {
        LabReviewResponseDto response = labReviewService.submit(member.getId(), request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 리뷰 생성 성공")
        );
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<LabReviewResponseDto>>> getLabReviews(
            @RequestParam Long laboratoryId
    ) {
        List<LabReviewResponseDto> reviews = labReviewService.getLabReviews(laboratoryId);

        return ResponseEntity.ok(
                ResponseDto.of(reviews, "연구실 리뷰 조회 성공")
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDto<LabReviewResponseDto>> getMyLabReview(
            @AuthenticationPrincipal Member member
    ) {
        LabReviewResponseDto response = labReviewService.getMyLabReview(member.getId());

        return ResponseEntity.ok(
                ResponseDto.of(response, "내 연구실 리뷰 조회 성공")
        );
    }

    @PatchMapping
    public ResponseEntity<ResponseDto<LabReviewResponseDto>> updateMyLabReview(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody LabReviewRequestDto request
    ) {
        LabReviewResponseDto response = labReviewService.updateMyLabReview(member.getId(), request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "내 연구실 리뷰 수정 성공")
        );
    }

    @DeleteMapping
    public ResponseEntity<ResponseDto<Long>> deleteMyLabReview(
            @AuthenticationPrincipal Member member
    ) {
        Long deletedReviewId = labReviewService.deleteMyLabReview(member.getId());

        return ResponseEntity.ok(
                ResponseDto.of(deletedReviewId, "내 연구실 리뷰 삭제 성공")
        );
    }
}
