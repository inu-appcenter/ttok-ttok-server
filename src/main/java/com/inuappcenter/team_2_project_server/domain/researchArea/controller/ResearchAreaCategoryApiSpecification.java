package com.inuappcenter.team_2_project_server.domain.researchArea.controller;

import com.inuappcenter.team_2_project_server.domain.researchArea.dto.ResearchAreaCategoryResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "연구분야 카테고리", description = "연구분야를 묶는 상위 카테고리 조회 API")
public interface ResearchAreaCategoryApiSpecification {

    @Operation(summary = "연구분야 카테고리 전체 조회", description = "등록된 상위 카테고리 목록을 전부 조회합니다. 개수가 적은 참조 데이터라 페이징하지 않습니다.")
    @ApiResponse(
            responseCode = "200",
            description = "연구분야 카테고리 전체 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                { "categoryName": "AI" },
                                { "categoryName": "로보틱스" }
                              ],
                              "code": null,
                              "message": "연구분야 카테고리 전체 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<ResearchAreaCategoryResponseDto>>> getAllResearchAreaCategories();

}
