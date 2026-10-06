package com.inuappcenter.team_2_project_server.domain.publication.controller;

import com.inuappcenter.team_2_project_server.domain.publication.dto.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "논문", description = "연구실 논문 조회 API")
public interface PublicationApiSpecification {

    @Operation(
            summary = "연구실 논문 목록 조회",
            description = """
                    연구실 ID로 그 연구실의 논문 목록을 페이지 단위로 조회합니다.
                    page(0-based) 쿼리 파라미터만 사용하며, 한 페이지당 5건으로 고정이고 연도 내림차순으로 정렬됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 논문 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "content": [
                                          {
                                            "id": 1,
                                            "title": "딥러닝 기반 이상 탐지 기법 연구",
                                            "researchersRaw": "홍길동, 이순신",
                                            "platform": "IEEE",
                                            "year": "2024",
                                            "type": "JOURNAL",
                                            "status": "PUBLISHED",
                                            "doi": "10.1000/example",
                                            "sourceURL": "https://doi.org/10.1000/example"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 5,
                                        "totalElements": 12,
                                        "totalPages": 3,
                                        "hasNext": true,
                                        "last": false
                                      },
                                      "code": null,
                                      "message": "연구실 논문 목록 조회 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 연구실",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "LABORATORY_NOT_FOUND",
                                      "message": "존재하지 않는 연구실입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<PageResponseDto<PublicationResponseDto>>> getPublications(
            @PathVariable Long laboratoryId,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page
    );
}
