package com.inuappcenter.team_2_project_server.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI 챗봇", description = "MindLogic FactChat 챗봇에 질문을 전달하고 답변을 받는 API")
public interface AiApiSpecification {

    @Operation(
            summary = "연구실 추천 챗봇에게 질문",
            description = """
                    인증된 사용자가 보낸 message 를 연구실 추천용 FactChat 챗봇에 그대로 전달하고, 첫 번째 응답 메시지를 answer 로 돌려줍니다.
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
                              "message": "머신러닝을 주제로 하는 연구실을 알려줘"
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
                                      "answer": "인공지능융합학과의 머신러닝 연구실을 추천합니다. ...",
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
    AiController.ChatResult researchLabChat(@RequestBody AiController.ChatRequest request);

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
