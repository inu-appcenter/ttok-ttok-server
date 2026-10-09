package com.inuappcenter.team_2_project_server.domain.recommendation.controller;

import com.inuappcenter.team_2_project_server.domain.recommendation.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "연구실 추천", description = "질문에 맞는 연구실을 AI로 추천하는 API")
public interface RecommendationApiSpecification {

    @Operation(
            summary = "연구실 추천",
            description = """
                    학생의 관심사 질문에 맞는 연구실을 최대 3개 추천합니다. 로그인한 유저만 사용할 수 있습니다.
                    - 연구실 소개와 논문을 검색해 후보를 찾고, AI가 후보 중에서 질문에 맞는 연구실을 골라 추천 이유를 씁니다.
                    - 확실히 맞는 연구실만 고르므로 3개보다 적을 수 있습니다.
                    - 맞는 연구실이 없으면 noMatch가 true이고 recommendations는 빈 목록입니다.
                    - AI 선택 단계가 실패하면 검색 순위 상위 3개를 돌려주며, 이때 reason은 null입니다.
                    - laboratoryId로 연구실 상세 화면으로 이동할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 추천 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "noMatch": false,
                                        "recommendations": [
                                          {
                                            "laboratoryId": 157,
                                            "labName": "무기나노촉매 연구실",
                                            "department": "화학과",
                                            "professorName": "권태현",
                                            "reason": "원자 수준으로 조성을 제어한 나노촉매로 수전해 수소 생산의 효율과 내구성을 높이는 연구를 합니다."
                                          }
                                        ]
                                      },
                                      "code": null,
                                      "message": "연구실 추천 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 장애 등으로 추천 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RECOMMENDATION_UNAVAILABLE",
                                      "message": "연구실 추천을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabRecommendationResponseDto>> recommendLabs(
            @RequestBody LabRecommendationRequestDto request
    );

    @Operation(
            summary = "추천 검색 문서 수동 동기화 (관리자 전용)",
            description = """
                    연구실 소개와 논문을 임베딩해 추천 검색 문서를 갱신합니다. 매일 새벽 5시 자동 배치와 별개로 지금 바로 반영할 때 사용합니다.
                    - 내용이 바뀐 연구실/논문만 다시 임베딩하고, 삭제된 연구실/논문의 문서는 지웁니다.
                    - 엑셀로 연구실 데이터를 새로 가져온 뒤나, 처음 배포한 뒤에 실행해야 추천이 동작합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "동기화 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "created": 12,
                                        "updated": 3,
                                        "deleted": 1,
                                        "failed": 0,
                                        "unchanged": 10414
                                      },
                                      "code": null,
                                      "message": "추천 검색 문서 동기화 완료"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabSearchIndexSyncResponseDto>> syncSearchIndex();
}
