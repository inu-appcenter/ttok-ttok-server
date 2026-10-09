package com.inuappcenter.team_2_project_server.domain.recommendation.controller;

import com.inuappcenter.team_2_project_server.domain.recommendation.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.LabRecommendationService;
import com.inuappcenter.team_2_project_server.domain.recommendation.service.LabSearchIndexService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendation")
public class RecommendationController implements RecommendationApiSpecification {

    private final LabRecommendationService labRecommendationService;
    private final LabSearchIndexService labSearchIndexService;

    /**
     * 연구실 추천 컨트롤러
     */
    @Override
    @PostMapping("/labs")
    public ResponseEntity<ResponseDto<LabRecommendationResponseDto>> recommendLabs(
            @Valid @RequestBody LabRecommendationRequestDto request
    ) {
        LabRecommendationResponseDto response = labRecommendationService.recommend(request.question());

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 추천 성공")
        );
    }

    /**
     * 추천 검색 문서 수동 동기화 컨트롤러 (관리자 전용, 매일 새벽 배치와 별개로 지금 바로 실행)
     */
    @Override
    @PostMapping("/index/sync")
    public ResponseEntity<ResponseDto<LabSearchIndexSyncResponseDto>> syncSearchIndex() {
        LabSearchIndexSyncResponseDto response = labSearchIndexService.syncAll();

        return ResponseEntity.ok(
                ResponseDto.of(response, "추천 검색 문서 동기화 완료")
        );
    }
}
