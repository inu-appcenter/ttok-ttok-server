package com.inuappcenter.team_2_project_server.domain.researchMetric.controller;

import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.request.ResearchMetricAuthorRequestDto;
import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.response.ResearchMetricResponseDto;
import com.inuappcenter.team_2_project_server.domain.researchMetric.service.ResearchMetricService;
import com.inuappcenter.team_2_project_server.domain.researchMetric.service.ResearchMetricSyncService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/research-metric")
public class ResearchMetricController implements ResearchMetricApiSpecification {

    private final ResearchMetricService researchMetricService;
    private final ResearchMetricSyncService researchMetricSyncService;

    /**
     * 연구실 연구 지표 조회 컨트롤러
     */
    @Override
    @GetMapping("/laboratory/{laboratoryId}/metrics")
    public ResponseEntity<ResponseDto<ResearchMetricResponseDto>> getLabMetrics(
            @PathVariable Long laboratoryId
    ) {
        ResearchMetricResponseDto response = researchMetricService.getLabMetrics(laboratoryId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 연구 지표 조회 성공")
        );
    }

    /**
     * 전체 연구 지표 수동 동기화 컨트롤러 (관리자 전용, 매일 새벽 배치와 별개로 지금 바로 실행)
     */
    @Override
    @PostMapping("sync")
    public ResponseEntity<ResponseDto<Void>> syncAllResearchMetrics() {
        researchMetricSyncService.syncAll();

        return ResponseEntity.ok(
                ResponseDto.of(null, "연구 지표 전체 동기화 완료")
        );
    }

    /**
     * 교수 OpenAlex 저자 ID 수동 지정 컨트롤러 (관리자 전용)
     */
    @Override
    @PatchMapping("professor/{professorId}/author")
    public ResponseEntity<ResponseDto<ResearchMetricResponseDto>> assignAuthor(
            @PathVariable Long professorId,
            @Valid @RequestBody ResearchMetricAuthorRequestDto request
    ) {
        ResearchMetricResponseDto response = researchMetricService.assignAuthor(professorId, request.openAlexAuthorId());

        return ResponseEntity.ok(
                ResponseDto.of(response, "OpenAlex 저자 지정 및 지표 갱신 완료")
        );
    }
}
