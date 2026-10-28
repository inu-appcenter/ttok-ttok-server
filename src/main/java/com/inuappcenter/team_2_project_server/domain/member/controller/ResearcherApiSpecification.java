package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ResearcherRegisterRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "연구자", description = "회원을 특정 연구실 소속 연구자로 등록·조회·해제하는 API")
public interface ResearcherApiSpecification {

    @Operation(
            summary = "연구자 등록",
            description = """
                    인증된 유저 본인을 지정한 연구실(laboratoryId) 소속 연구자로 등록합니다.
                    등록 대상 회원은 인증 토큰에서 가져오므로 요청 본문에 memberId를 넣지 않습니다.
                    회원당 1명의 연구자만 등록할 수 있고, 교수 회원은 등록할 수 없습니다.
                    등록 후 유저 타입은 RESEARCHER(연구생)로 변경됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "소속 연구실 ID",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResearcherRegisterRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "laboratoryId": 465
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구자 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "memberId": 1,
                                        "studentNumber": "20240001",
                                        "laboratoryId": 465,
                                        "laboratoryName": "소프트웨어공학 연구실",
                                        "validateYN": false
                                      },
                                      "code": null,
                                      "message": "연구자 등록 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패, 이미 등록된 연구자 또는 교수 회원",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "이미 등록된 연구자",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "RESEARCHER_ALREADY_EXISTS",
                                                      "message": "이미 존재하는 연구자입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "교수 회원",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "ALREADY_PROFESSOR",
                                                      "message": "교수는 연구자가 될 수 없습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 누락, 만료 또는 유효하지 않은 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_INVALID",
                                      "message": "유효하지 않은 토큰입니다."
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
    ResponseEntity<ResponseDto<ResearcherResponseDto>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ResearcherRegisterRequestDto request
    );

    @Operation(
            summary = "연구자 조회",
            description = "회원 ID로 해당 회원의 연구자 등록 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구자 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "memberId": 1,
                                        "studentNumber": "20240001",
                                        "laboratoryId": 465,
                                        "laboratoryName": "소프트웨어공학 연구실",
                                        "validateYN": false
                                      },
                                      "code": null,
                                      "message": "연구자 조회 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 회원의 연구자 등록 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RESEARCHER_NOT_FOUND",
                                      "message": "존재하지 않는 연구생입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<ResearcherResponseDto>> getResearcher(
            @Parameter(description = "조회할 회원 ID", required = true, example = "1")
            @RequestParam Long memberId
    );

    @Operation(
            summary = "내 연구자 등록 해제",
            description = """
                    인증된 유저 본인의 연구자 등록을 해제합니다. 회원 계정은 그대로 남고 연구자 정보만 삭제됩니다.
                    해제 시 본인이 등록한 커피챗은 삭제되고, 작성한 연구실 리뷰는 삭제되지 않고 작성자 연결만 끊겨 익명으로 남습니다.
                    해제 후 유저 타입은 FINDER(탐색자)로 변경됩니다. 응답 data는 해제한 유저의 ID입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구자 등록 해제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "1번 유저 연구자 삭제 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 누락, 만료 또는 유효하지 않은 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_INVALID",
                                      "message": "유효하지 않은 토큰입니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "연구자로 등록되어 있지 않은 유저",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RESEARCHER_NOT_FOUND",
                                      "message": "존재하지 않는 연구생입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteResearcher(
            @AuthenticationPrincipal Member member
    );
}
