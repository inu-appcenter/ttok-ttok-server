package com.inuappcenter.team_2_project_server.domain.ai.controller;

import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabKeywordSuggestionRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabKeywordSuggestionResponseDto;
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
                    학생의 관심사(interest)와 고른 키워드(selectedKeywords)에 맞는 연구실을 최대 3개 추천합니다. 로그인한 유저만 사용할 수 있습니다.
                    - 연구실 소개와 논문을 검색해 후보를 찾고(RAG), AI가 후보 중에서 요청에 맞는 연구실을 골라 추천 이유를 씁니다.
                    - 확실히 맞는 연구실만 고르므로 3개보다 적을 수 있습니다. 첫 번째가 가장 잘 맞는 연구실입니다.
                    - 맞는 연구실이 없으면 noMatch가 true이고 recommendations는 빈 목록입니다.
                    - keywords는 연구실 연구분야 태그, reason은 한 줄짜리 추천 이유입니다.
                    - AI 선택 단계가 실패하면 검색 순위 상위 3개를 돌려주며, 이때 reason은 null입니다.
                    - laboratoryId로 연구실 상세 화면으로 이동할 수 있습니다.
                    - interest는 비어 있으면 안 되고 최대 300자입니다.
                    - interest는 키워드 추천 API(POST /api/chat/research-lab/keywords)에 보낸 것과 같은 값을 보냅니다.
                    - selectedKeywords는 키워드 추천 API로 받은 키워드 중 고른 것과 직접 입력한 키워드입니다.
                      최대 3개, 각 최대 30자이며 생략하거나 빈 목록이어도 됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "학생의 관심사와 고른 키워드",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabRecommendationRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "interest": "물을 전기분해해서 그린수소 만드는 촉매를 연구하고 싶어요",
                              "selectedKeywords": ["에너지", "촉매"]
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
                                            "keywords": ["나노촉매", "수전해", "에너지 소재"],
                                            "reason": "나노촉매로 수전해 수소 생산 효율을 높이는 연구를 해요"
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
            summary = "연구실 추천 키워드",
            description = """
                    학생이 자유롭게 적은 관심사(interest)를 보고, 고를 만한 연구분야 키워드를 최대 5개 돌려줍니다. 로그인한 유저만 사용할 수 있습니다.
                    - 관심사와 가까운 연구실들을 찾아, 그 연구실들이 가진 연구분야 카테고리를 많이 나온 순으로 돌려줍니다.
                    - 실제 연구실이 가진 카테고리만 나오므로, 어떤 키워드를 골라도 추천할 연구실이 있습니다.
                    - 맞는 연구실을 찾지 못하면 keywords는 빈 목록입니다. 이때는 직접 입력으로 진행하면 됩니다.
                    - 연구실 추천 API(POST /api/chat/research-lab)를 호출할 때 같은 interest와, 고른 키워드를 selectedKeywords로 보냅니다.
                    - interest는 비어 있으면 안 되고 최대 300자입니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "학생의 관심사",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabKeywordSuggestionRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "interest": "추천시스템에 관심이 있고, 파이썬으로 크롤링 프로젝트를 해봤어요."
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 추천 키워드 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "keywords": ["AI", "데이터", "웹", "알고리즘", "소프트웨어공학"]
                                      },
                                      "code": null,
                                      "message": "연구실 추천 키워드 조회 성공"
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
                    description = "AI 서비스(임베딩) 장애로 키워드 추천 불가",
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
    ResponseEntity<ResponseDto<LabKeywordSuggestionResponseDto>> suggestResearchLabKeywords(@RequestBody LabKeywordSuggestionRequestDto request);

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
