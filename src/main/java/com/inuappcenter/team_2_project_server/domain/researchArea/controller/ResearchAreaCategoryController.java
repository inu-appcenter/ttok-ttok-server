package com.inuappcenter.team_2_project_server.domain.researchArea.controller;

import com.inuappcenter.team_2_project_server.domain.researchArea.dto.ResearchAreaCategoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.researchArea.service.ResearchAreaCategoryService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
