package com.inuappcenter.team_2_project_server.domain.laboratory.controller;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.ResearchAreaCategoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchAreaCategoryService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/research-area-category")
public class ResearchAreaCategoryController implements ResearchAreaCategoryApiSpecification {
    private final ResearchAreaCategoryService researchAreaCategoryService;

    @Override
    @GetMapping
    public ResponseEntity<ResponseDto<List<ResearchAreaCategoryResponseDto>>> getAllResearchAreaCategories() {
        List<ResearchAreaCategoryResponseDto> responses = researchAreaCategoryService.getAllResearchAreaCategory();

        return ResponseEntity.ok(
                ResponseDto.of(responses, "연구분야 카테고리 전체 조회 성공")
        );
    }

    @Override
    @GetMapping("/{categoryName}")
    public ResponseEntity<ResponseDto<ResearchAreaCategoryResponseDto>> getResearchAreaCategory(
            @PathVariable String categoryName
    ) {
        ResearchAreaCategoryResponseDto response = researchAreaCategoryService.getResearchAreaCategory(categoryName);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구분야 카테고리 조회 성공")
        );
    }
}
