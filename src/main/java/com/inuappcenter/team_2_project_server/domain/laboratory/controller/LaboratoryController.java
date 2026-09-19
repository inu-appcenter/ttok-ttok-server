package com.inuappcenter.team_2_project_server.domain.laboratory.controller;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryExcelImportService;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/laboratory")
public class LaboratoryController implements LaboratoryApiSpecification {
    private final LaboratoryExcelImportService laboratoryExcelImportService;
    private final LaboratoryService laboratoryService;

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
            @Valid @RequestBody LaboratoryCreateRequestDto request
    ) {
        LaboratoryResponseDto response = laboratoryService.createLab(request);

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
            @ParameterObject
            @PageableDefault(size = 20, sort = "labName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<LaboratoryResponseDto> page = laboratoryService.getAllLab(pageable);

        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(page), "전체 연구실 조회 성공")
        );
    }

    @PatchMapping("/{laboratoryId}")
    public ResponseEntity<ResponseDto<LaboratoryResponseDto>> updateLaboratory(
            @PathVariable Long laboratoryId,
            @Valid @RequestBody LaboratoryUpdateRequestDto request
    ) {
        LaboratoryResponseDto response = laboratoryService.updateLab(laboratoryId, request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 수정 성공")
        );
    }

    @DeleteMapping("/{laboratoryId}")
    public ResponseEntity<ResponseDto<Long>> deleteLaboratory(
            @PathVariable Long laboratoryId
    ) {
        laboratoryService.deleteLab(laboratoryId);
        return ResponseEntity.ok(
                ResponseDto.of(laboratoryId, "연구실 삭제 완료")
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> searchLaboratory(
            @RequestParam String keyword,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<LaboratoryResponseDto> page = laboratoryService.searchLabs(keyword, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(page), "연구실 검색 성공")
        );
    }

    @GetMapping("/search/category")
    public ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> searchLaboratoryByCategory(
            @RequestParam String categoryName,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<LaboratoryResponseDto> page = laboratoryService.searchLabsByCategory(categoryName, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(page), "카테고리별 연구실 검색 성공")
        );
    }
}
