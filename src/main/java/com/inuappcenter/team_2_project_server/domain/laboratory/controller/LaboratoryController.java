package com.inuappcenter.team_2_project_server.domain.laboratory.controller;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LabCountByCollegeResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.ResearchProjectResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryExcelImportService;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchProjectSyncService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/laboratory")
public class LaboratoryController implements LaboratoryApiSpecification {
    private final LaboratoryExcelImportService laboratoryExcelImportService;
    private final LaboratoryService laboratoryService;
    private final ResearchProjectSyncService researchProjectSyncService;

    @Override
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Void>> importLaboratory(@RequestPart MultipartFile file) {
        laboratoryExcelImportService.importExcel(file);
        return ResponseEntity.ok(
                ResponseDto.of(null, "연구실 편람 동기화 완료")
        );
    }

    @PostMapping
    public ResponseEntity<ResponseDto<LaboratoryResponseDto>> createLaboratory(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody LaboratoryCreateRequestDto request
    ) {
        LaboratoryResponseDto response = laboratoryService.createLab(member, request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 생성 성공")
        );
    }


    @GetMapping("/{laboratoryId}")
    public ResponseEntity<ResponseDto<LaboratoryResponseDto>> getLaboratory(
            @PathVariable Long laboratoryId
    ) {
        LaboratoryResponseDto response = laboratoryService.getLab(laboratoryId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 조회 성공")
        );
    }

    @GetMapping
    public ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> getAllLaboratory(
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(validatePage(page), 20, Sort.by(Sort.Direction.ASC, "labName"));
        Page<LaboratoryResponseDto> result = laboratoryService.getAllLab(pageable);

        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "전체 연구실 조회 성공")
        );
    }

    @PatchMapping("/{laboratoryId}")
    public ResponseEntity<ResponseDto<LaboratoryResponseDto>> updateLaboratory(
            @AuthenticationPrincipal Member member,
            @PathVariable Long laboratoryId,
            @Valid @RequestBody LaboratoryUpdateRequestDto request
    ) {
        LaboratoryResponseDto response = laboratoryService.updateLab(laboratoryId, request, member);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 수정 성공")
        );
    }

    @DeleteMapping("/{laboratoryId}")
    public ResponseEntity<ResponseDto<Long>> deleteLaboratory(
            @AuthenticationPrincipal Member member,
            @PathVariable Long laboratoryId
    ) {
        laboratoryService.deleteLab(member, laboratoryId);
        return ResponseEntity.ok(
                ResponseDto.of(laboratoryId, "연구실 삭제 완료")
        );
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> searchLaboratory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page
    ) {
        // 페이지를 넘겨도 순서가 바뀌지 않도록 이름순 + id로 정렬 고정
        Pageable pageable = PageRequest.of(validatePage(page), 20, Sort.by(Sort.Direction.ASC, "labName").and(Sort.by("id")));
        Page<LaboratoryResponseDto> result = laboratoryService.searchLabs(keyword, category, college, department, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "연구실 검색 성공")
        );
    }

    @Override
    @GetMapping("/{laboratoryId}/publications")
    public ResponseEntity<ResponseDto<PageResponseDto<PublicationResponseDto>>> getPublications(
            @PathVariable Long laboratoryId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(validatePage(page), 5, Sort.by(Sort.Direction.DESC, "year"));
        Page<PublicationResponseDto> result = laboratoryService.getLabPublications(laboratoryId, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "연구실 논문 목록 조회 성공")
        );
    }

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
        Page<ResearchProjectResponseDto> result = laboratoryService.getLabResearchProjects(laboratoryId, pageable);
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

    @Override
    @GetMapping("/college-department/count")
    public ResponseEntity<ResponseDto<List<LabCountByCollegeResponseDto>>> getLabCountByCollegeDept() {
        List<LabCountByCollegeResponseDto> responses = laboratoryService.getLabByCollegeDeptCount();

        return ResponseEntity.ok(
                ResponseDto.of(responses, "전체 단과대/학과별 연구실 갯수 조회 성공")
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
