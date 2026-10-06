package com.inuappcenter.team_2_project_server.domain.labProject.controller;

import com.inuappcenter.team_2_project_server.domain.labProject.dto.ResearchProjectAssignRequestDto;
import com.inuappcenter.team_2_project_server.domain.labProject.dto.ResearchProjectResponseDto;
import com.inuappcenter.team_2_project_server.domain.labProject.service.ResearchProjectService;
import com.inuappcenter.team_2_project_server.domain.labProject.service.ResearchProjectSyncService;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// URL은 연구실 하위 리소스(/api/laboratory/...)라 기존 경로를 그대로 유지 (SecurityConfig 규칙도 그대로 적용됨)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/laboratory")
public class ResearchProjectController implements ResearchProjectApiSpecification {
    private final ResearchProjectService researchProjectService;
    private final ResearchProjectSyncService researchProjectSyncService;

    /**
     * 연구과제 조회 컨트롤러
     */
    @Override
    @GetMapping("/{laboratoryId}/research-projects")
    public ResponseEntity<ResponseDto<PageResponseDto<ResearchProjectResponseDto>>> getResearchProjects(
            @PathVariable Long laboratoryId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(
                validatePage(page), 5, Sort.by(Sort.Direction.DESC, "ongoing").and(Sort.by(Sort.Direction.DESC, "projectYear"))
        );
        Page<ResearchProjectResponseDto> result = researchProjectService.getLabResearchProjects(laboratoryId, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "연구실 연구과제 목록 조회 성공")
        );
    }

    /**
     * 전체 연구실 연구과제 수동 동기화 컨트롤러 (관리자 전용, 매일 새벽 배치와 별개로 지금 바로 실행)
     */
    @Override
    @PostMapping("/research-projects/sync")
    public ResponseEntity<ResponseDto<Void>> syncAllResearchProjects() {
        researchProjectSyncService.syncAll();
        return ResponseEntity.ok(
                ResponseDto.of(null, "연구과제 전체 동기화 완료")
        );
    }

    /**
     * 연구실 매핑이 보류된 연구과제 목록 조회 컨트롤러 (관리자 전용)
     */
    @Override
    @GetMapping("/research-projects/pending")
    public ResponseEntity<ResponseDto<PageResponseDto<ResearchProjectResponseDto>>> getPendingResearchProjects(
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(validatePage(page), 20, Sort.by(Sort.Direction.ASC, "managerName").and(Sort.by("id")));
        Page<ResearchProjectResponseDto> result = researchProjectService.getPendingResearchProjects(pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "매핑 보류 연구과제 목록 조회 성공")
        );
    }

    /**
     * 연구과제 연구실 수동 지정 컨트롤러 (관리자 전용)
     */
    @Override
    @PatchMapping("/research-projects/{researchProjectId}/laboratory")
    public ResponseEntity<ResponseDto<ResearchProjectResponseDto>> assignResearchProjectLaboratory(
            @PathVariable Long researchProjectId,
            @Valid @RequestBody ResearchProjectAssignRequestDto request
    ) {
        ResearchProjectResponseDto response = researchProjectService.assignResearchProjectLaboratory(researchProjectId, request.laboratoryId());
        return ResponseEntity.ok(
                ResponseDto.of(response, "연구과제 연구실 지정 완료")
        );
    }

    // 음수 페이지가 들어오면 PageRequest에서 500이 나므로 400으로 막음
    private int validatePage(int page) {
        if (page < 0) {
            throw new MyException(ErrorCode.INVALID_INPUT);
        }
        return page;
    }
}
