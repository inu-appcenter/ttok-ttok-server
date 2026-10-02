package com.inuappcenter.team_2_project_server.domain.researchMetric.controller;

import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.request.ResearchMetricAuthorRequestDto;
import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.response.ResearchMetricResponseDto;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "연구 지표", description = "교수 연구 지표(h-index, 피인용, 최근 5년 논문) 조회 및 OpenAlex 동기화 API")
public interface ResearchMetricApiSpecification {

    @Operation(
            summary = "연구실 연구 지표 조회",
            description = """
                    연구실 담당 교수의 연구 지표를 조회합니다. 인증 없이 조회 가능합니다.
                    - hIndex, citationCount: 매일 새벽 OpenAlex에서 동기화한 값입니다. 교수와 OpenAlex 저자 매칭 전이면 null 입니다.
                    - recentPublicationCount: 서비스에 등록된 논문 중 최근 5년(올해 포함) 논문 수입니다.
                    - syncedAt: 마지막으로 OpenAlex에서 지표를 받아온 시각입니다. (툴팁의 기준 시점 표기용)
                    - 담당 교수가 없는 연구실이면 data 가 null 입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 연구 지표 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "professorId": 1,
                                        "professorName": "홍길동",
                                        "hIndex": 14,
                                        "citationCount": 1240,
                                        "recentPublicationCount": 23,
                                        "syncedAt": "2026-10-02T04:30:12"
                                      },
                                      "code": null,
                                      "message": "연구실 연구 지표 조회 성공"
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
    ResponseEntity<ResponseDto<ResearchMetricResponseDto>> getLabMetrics(
            @Parameter(description = "연구실 id", required = true, example = "1")
            @PathVariable Long laboratoryId
    );

    @Operation(
            summary = "전체 연구 지표 수동 동기화 (관리자 전용)",
            description = """
                    연구실이 있는 모든 교수에 대해 OpenAlex에서 연구 지표를 즉시 동기화합니다.
                    매일 새벽 자동 배치와 별개로, 지금 바로 반영이 필요할 때 사용합니다.
                    - 저자 매칭이 안 된 교수는 최근 논문 DOI로 OpenAlex 저자 매칭을 먼저 시도합니다.
                    - 교수 수만큼 외부 API를 순차 호출하므로 응답까지 수 분이 걸릴 수 있습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "전체 동기화 완료",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": null,
                              "code": null,
                              "message": "연구 지표 전체 동기화 완료"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<Void>> syncAllResearchMetrics();

    @Operation(
            summary = "교수 OpenAlex 저자 ID 수동 지정 (관리자 전용)",
            description = """
                    자동 매칭이 보류됐거나 잘못 매칭된 교수의 OpenAlex 저자 ID를 직접 지정하고, 바로 지표를 받아옵니다.
                    직접 지정한 저자 ID는 이후 자동 매칭 배치가 덮어쓰지 않습니다.
                    저자 ID는 OpenAlex 웹사이트(openalex.org)에서 교수를 검색해 확인할 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "OpenAlex 저자 ID (A5067381195 또는 https://openalex.org/A5067381195 형식)",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResearchMetricAuthorRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "openAlexAuthorId": "A5067381195"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저자 지정 및 지표 갱신 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "professorId": 1,
                                        "professorName": "홍길동",
                                        "hIndex": 14,
                                        "citationCount": 1240,
                                        "recentPublicationCount": 23,
                                        "syncedAt": "2026-10-02T15:10:03"
                                      },
                                      "code": null,
                                      "message": "OpenAlex 저자 지정 및 지표 갱신 완료"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 교수 또는 저자 ID 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "INVALID_INPUT",
                                      "message": "잘못된 요청입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<ResearchMetricResponseDto>> assignAuthor(
            @Parameter(description = "교수 id", required = true, example = "1")
            @PathVariable Long professorId,
            @RequestBody ResearchMetricAuthorRequestDto request
    );
}
