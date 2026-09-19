package com.inuappcenter.team_2_project_server.domain.laboratory.controller;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.ResearchAreaCategoryResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

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

    @Operation(summary = "연구분야 카테고리 단건 조회", description = "카테고리명으로 카테고리 하나를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구분야 카테고리 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": { "categoryName": "AI" },
                                      "code": null,
                                      "message": "연구분야 카테고리 조회 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 카테고리",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RESEARCH_AREA_CATEGORY_NOT_FOUND",
                                      "message": "해당 카테고리가 존재하지 않습니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<ResearchAreaCategoryResponseDto>> getResearchAreaCategory(
            @Parameter(description = "조회할 카테고리명", required = true, example = "AI")
            @PathVariable String categoryName
    );
}
