package com.inuappcenter.team_2_project_server.domain.ai.controller;

import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabSearchIndexSyncResponseDto;
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

@Tag(name = "AI", description = "연구실 추천(RAG)과 이메일 작성 도우미(FactChat 챗봇) API")
public interface AiApiSpecification {

    @Operation(
            summary = "연구실 추천",
            description = """
                    학생의 관심사 질문(message)에 맞는 연구실을 최대 3개 추천합니다. 로그인한 유저만 사용할 수 있습니다.
                    - 연구실 소개와 논문을 검색해 후보를 찾고(RAG), AI가 후보 중에서 질문에 맞는 연구실을 골라 추천 이유를 씁니다.
                    - 확실히 맞는 연구실만 고르므로 3개보다 적을 수 있습니다.
                    - 맞는 연구실이 없으면 noMatch가 true이고 recommendations는 빈 목록입니다.
                    - AI 선택 단계가 실패하면 검색 순위 상위 3개를 돌려주며, 이때 reason은 null입니다.
                    - laboratoryId로 연구실 상세 화면으로 이동할 수 있습니다.
                    - message는 비어 있으면 안 되고 최대 500자입니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "학생의 관심사 질문",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabRecommendationRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "물을 전기분해해서 그린수소 만드는 촉매를 연구하고 싶어요"
                            }
                            """)
            )
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
                    responseCode = "401",
                    description = "인증 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_MISSING",
                                      "message": "인증 토큰이 필요합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 장애, 검색 문서 미생성 등으로 추천 불가",
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
    ResponseEntity<ResponseDto<LabRecommendationResponseDto>> researchLabChat(@RequestBody LabRecommendationRequestDto request);

    @Operation(
            summary = "연구실 추천 검색 문서 수동 동기화 (관리자 전용)",
            description = """
                    연구실 소개와 논문을 임베딩해 추천 검색 문서를 갱신합니다. 매일 새벽 5시 자동 배치와 별개로 지금 바로 반영할 때 사용합니다.
                    - 내용이 바뀐 연구실/논문만 다시 임베딩하고, 삭제된 연구실/논문의 문서는 지웁니다.
                    - 엑셀로 연구실 데이터를 새로 가져온 뒤나, 처음 배포한 뒤에 실행해야 추천이 동작합니다.
                    - 처음 실행할 때는 전체를 임베딩하므로 몇 분 걸립니다.
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
                                      "message": "연구실 추천 검색 문서 동기화 완료"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabSearchIndexSyncResponseDto>> syncResearchLabIndex();

    @Operation(
            summary = "이메일 작성 도우미 챗봇에게 질문",
            description = """
                    인증된 사용자가 보낸 message 를 이메일 작성/첨삭용 FactChat 챗봇에 그대로 전달하고, 첫 번째 응답 메시지를 answer 로 돌려줍니다.
                    대화 이력은 서버에 저장하지 않으며, 매 요청이 독립적인 단발성 질문으로 처리됩니다.
                    외부 AI 서버 호출이 동기로 이뤄지므로 응답까지 수 초가 걸릴 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "챗봇에게 보낼 질문 문자열",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AiController.ChatRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "교수님께 보낼 면담 요청 메일을 정중하게 작성해줘"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 응답 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiController.ChatResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "answer": "안녕하세요 교수님, ...",
                                      "credits": 987.5
                                    }
                                    """)
            )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.inuappcenter.team_2_project_server.global.dto.ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_MISSING",
                                      "message": "인증 토큰이 필요합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "외부 AI 서버 호출 실패 또는 빈 응답",
                    content = @Content(mediaType = "application/json")
            )
    })
    AiController.ChatResult emailEditorChat(@RequestBody AiController.ChatRequest request);
}
