package com.inuappcenter.team_2_project_server.domain.coffeeChat.controller;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatResponseDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatUpdateRequestDto;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "커피챗", description = "연구실 커피챗 신청 및 관리 API")
public interface CoffeeChatApiSpecification {

    @Operation(
            summary = "연구실 커피챗 목록 조회",
            description = "연구실 ID(쿼리 파라미터)로 해당 연구실에 신청된 커피챗 목록을 조회합니다. 로그인한 사용자면 누구나 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 커피챗 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": [
                                        {
                                          "id": 1,
                                          "laboratoryId": 1,
                                          "laboratoryName": "소프트웨어공학 연구실",
                                          "researcherId": 1,
                                          "contactType": "KAKAO_TALK",
                                          "contactValue": "https://open.kakao.com/o/gABCdef"
                                        }
                                      ],
                                      "code": null,
                                      "message": "연구실 커피챗 목록 조회 성공"
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
            )
    })
    ResponseEntity<ResponseDto<List<CoffeeChatResponseDto>>> getAllLabCoffeeChat(
            @Parameter(description = "커피챗 목록을 조회할 연구실 ID", required = true, example = "1")
            @RequestParam Long laboratoryId
    );

    @Operation(
            summary = "내 커피챗 조회",
            description = "인증된 사용자가 신청한 커피챗을 조회합니다. 커피챗은 연구생당 1개만 존재합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 커피챗 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "laboratoryId": 1,
                                        "laboratoryName": "소프트웨어공학 연구실",
                                        "researcherId": 1,
                                        "contactType": "KAKAO_TALK",
                                        "contactValue": "https://open.kakao.com/o/gABCdef"
                                      },
                                      "code": null,
                                      "message": "내 커피챗 조회 성공"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<CoffeeChatResponseDto>> getMyCoffeeChat(
            @AuthenticationPrincipal Member member
    );

    @Operation(
            summary = "커피챗 생성",
            description = """
                    인증된 사용자가 특정 연구실에 커피챗을 신청합니다.
                    신청자는 토큰에서 식별하며, 연구생당 1개의 커피챗만 생성할 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "신청할 연구실 ID와 연락 수단, 신청 내용",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CoffeeChatCreateRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "laboratoryId": 1,
                              "contactType": "KAKAO_TALK",
                              "contactValue": "https://open.kakao.com/o/gABCdef"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "커피챗 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "laboratoryId": 1,
                                        "laboratoryName": "소프트웨어공학 연구실",
                                        "researcherId": 1,
                                        "contactType": "KAKAO_TALK",
                                        "contactValue": "https://open.kakao.com/o/gABCdef"
                                      },
                                      "code": null,
                                      "message": "커피챗 생성 성공"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<CoffeeChatResponseDto>> createCoffeeChat(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody CoffeeChatCreateRequestDto request
    );

    @Operation(
            summary = "커피챗 수정",
            description = "커피챗 ID로 연락 수단과 신청 내용을 수정합니다. 본인이 신청한 커피챗만 수정할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 연락 수단과 신청 내용",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CoffeeChatUpdateRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "contactType": "EMAIL",
                              "contactValue": "researcher@inu.ac.kr"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "커피챗 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "laboratoryId": 1,
                                        "laboratoryName": "소프트웨어공학 연구실",
                                        "researcherId": 1,
                                        "contactType": "EMAIL",
                                        "contactValue": "researcher@inu.ac.kr"
                                      },
                                      "code": null,
                                      "message": "커피챗 수정 성공"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<CoffeeChatResponseDto>> updateCoffeeChat(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody CoffeeChatUpdateRequestDto request,
            @Parameter(description = "수정할 커피챗 ID", required = true, example = "1")
            @PathVariable Long coffeeChatId
    );

    @Operation(
            summary = "커피챗 삭제",
            description = "커피챗 ID로 커피챗을 삭제합니다. 본인이 신청한 커피챗만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "커피챗 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "커피챗 삭제 성공"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteCoffeeChat(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "삭제할 커피챗 ID", required = true, example = "1")
            @PathVariable Long coffeeChatId
    );
}
