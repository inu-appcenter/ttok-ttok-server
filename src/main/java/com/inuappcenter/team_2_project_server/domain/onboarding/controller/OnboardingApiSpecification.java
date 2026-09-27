package com.inuappcenter.team_2_project_server.domain.onboarding.controller;

import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.onboarding.dto.OnboardingRequestDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "온보딩", description = "첫 로그인 후 진행하는 온보딩 답변을 한 번에 저장하는 API")
public interface OnboardingApiSpecification {

    @Operation(
            summary = "온보딩 완료",
            description = """
                    온보딩에서 선택한 답변을 한 번에 받아 저장하고, 회원의 isNew 플래그를 false 로 내립니다.
                    전체가 하나의 트랜잭션이라 도중에 하나라도 실패하면 아무것도 저장되지 않습니다.

                    - purpose = RESEARCHER: 소속 연구실로 연구자 등록 + 연구실 리뷰 저장, coffeeChatAllowed 가 true 면 커피챗까지 생성
                    - purpose = FINDER: 별도 저장 없이 온보딩만 완료 (laboratoryId 등 나머지 필드는 무시)
                    - purpose = PROFESSOR: 교수 계정(로그인 시 학번/교번 자릿수로 자동 분류됨)이 사용. 다른 필드는 다 무시하고
                      professorDepartment + professorName 으로 아직 아무 계정과도 연동되지 않은 교수 레코드를 찾아 자동으로 연동합니다
                      (이름만으로는 동명이인이 있을 수 있어 학과까지 받습니다). 연동되는 순간 그 교수가 담당하는 연구실의 수정 권한도 함께 생기고,
                      회원의 department 도 이 값으로 채워집니다. 그래도 같은 학과에 동명이인이 있으면 자동 연동에 실패합니다.

                    coreTime / weeklyMeeting / doings 는 연구실 리뷰 기본 선택지 값을 그대로 보냅니다. ("있음", "주 1회" 등)
                    커피챗 연락처는 EMAIL 형식이거나 https://open.kakao.com/ 로 시작하는 링크여야 합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OnboardingRequestDto.class),
                    examples = {
                            @ExampleObject(name = "연구생 + 커피챗 허용", value = """
                                    {
                                      "purpose": "RESEARCHER",
                                      "laboratoryId": 465,
                                      "coreTime": "있음",
                                      "weeklyMeeting": "주 1회",
                                      "doings": ["논문 리딩", "실험/코딩", "세미나 발표"],
                                      "coffeeChatAllowed": true,
                                      "contactType": "KAKAO_TALK",
                                      "contactValue": "https://open.kakao.com/o/abcd1234"
                                    }
                                    """),
                            @ExampleObject(name = "연구생 + 커피챗 미허용", value = """
                                    {
                                      "purpose": "RESEARCHER",
                                      "laboratoryId": 465,
                                      "coreTime": "없음",
                                      "weeklyMeeting": "격주",
                                      "doings": ["데이터 라벨링"],
                                      "coffeeChatAllowed": false
                                    }
                                    """),
                            @ExampleObject(name = "연구실 탐색만 (FINDER)", value = """
                                    {
                                      "purpose": "FINDER",
                                      "coffeeChatAllowed": false
                                    }
                                    """),
                            @ExampleObject(name = "교수 (본인 학과+이름으로 연동)", value = """
                                    {
                                      "purpose": "PROFESSOR",
                                      "professorDepartment": "COMPUTER_ENGINEERING",
                                      "professorName": "홍길동"
                                    }
                                    """)
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "온보딩 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "studentNumber": "20240001",
                                        "nickName": "홍길동",
                                        "department": "COMPUTER_ENGINEERING",
                                        "email": "student@example.com",
                                        "lastLoginAt": "2026-09-04T12:00:00",
                                        "isNew": false,
                                        "userType": "RESEARCHER"
                                      },
                                      "code": null,
                                      "message": "온보딩 완료"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 / 이미 온보딩 완료 / 이미 등록된 연구자 / 이미 작성한 리뷰 / 이미 생성한 커피챗 / 학과+이름과 일치하는 교수 없음 또는 동명이인으로 자동 연동 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "이미 온보딩 완료",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "ONBOARDING_ALREADY_DONE",
                                                      "message": "이미 온보딩을 완료했습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "일치하는 교수 없음",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "PROFESSOR_NOT_FOUND",
                                                      "message": "존재하지 않는 교수입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "동명이인으로 자동 연동 실패",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "PROFESSOR_NAME_AMBIGUOUS",
                                                      "message": "동일한 이름의 교수가 여러 명 있어 자동으로 연동할 수 없습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원 또는 연구실을 찾을 수 없음",
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
    ResponseEntity<ResponseDto<MemberResponseDto>> complete(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody OnboardingRequestDto request
    );
}
