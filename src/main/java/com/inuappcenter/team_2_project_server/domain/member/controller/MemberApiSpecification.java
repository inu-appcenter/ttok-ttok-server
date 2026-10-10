package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.LoginRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.MemberCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.MemberUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.TokenReissueRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.LoginResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.MemberResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "회원", description = "회원 인증 및 프로필 관리 API")
public interface MemberApiSpecification {

    @Operation(
            summary = "로그인",
            description = """
                    학번과 비밀번호로 로그인하고 JWT 토큰을 발급합니다.
                    미가입 학번이면 회원을 생성하며 이 경우 isNew=true 로 내려갑니다.
                    isNew 는 온보딩 미완료 여부이며, 온보딩(POST /api/onboarding)을 마치면 false 로 내려갑니다.
                    accessToken 만료 시에는 POST /api/member/reissue 로 refreshToken 을 보내 재발급받습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "accessTokenExpiresAt": "2026-08-18T12:00:00",
                                        "refreshTokenExpiresAt": "2026-08-25T12:00:00",
                                        "memberId": 1,
                                        "isNew": true
                                      },
                                      "code": null,
                                      "message": "로그인 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
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
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "학번 또는 비밀번호 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "INVALID_CREDENTIALS",
                                      "message": "학번 또는 비밀번호가 올바르지 않습니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request
    );

    @Operation(
            summary = "토큰 재발급",
            description = """
                    로그인 시 받은 refreshToken 으로 새 accessToken 과 refreshToken 을 재발급합니다.
                    Authorization 헤더는 보내지 않고 refreshToken 만 본문에 담아 호출합니다.
                    새 refreshToken 이 함께 내려오므로 클라이언트는 저장값을 교체해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "accessTokenExpiresAt": "2026-09-06T13:00:00",
                                        "refreshTokenExpiresAt": "2026-09-20T12:00:00",
                                        "memberId": 1,
                                        "isNew": false
                                      },
                                      "code": null,
                                      "message": "토큰 재발급 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "refreshToken 이 없거나 만료 또는 유효하지 않음 (accessToken/다른 타입 토큰 포함)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_EXPIRED",
                                      "message": "만료된 토큰입니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "토큰의 회원이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "MEMBER_NOT_FOUND",
                                      "message": "존재하지 않는 유저입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LoginResponseDto>> reissue(
            @Valid @RequestBody TokenReissueRequestDto request
    );

    @Operation(
            summary = "로그아웃",
            description = """
                    유효한 accessToken 으로 호출합니다. (Authorization 헤더 필요)
                    호출 시점 이전에 발급된 이 회원의 모든 accessToken/refreshToken 이 즉시 무효화됩니다. (전 기기 로그아웃)
                    클라이언트는 저장된 토큰을 삭제해야 합니다. 응답 data 는 회원 ID 입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "로그아웃 성공"
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
    ResponseEntity<ResponseDto<Long>> logout(
            @AuthenticationPrincipal Member member
    );

    @Operation(summary = "유저 생성", description = "관리 목적의 유저 계정을 생성합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 생성 성공",
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
                                        "lastLoginAt": "2026-08-18T12:00:00",
                                        "isNew": true
                                      },
                                      "code": null,
                                      "message": "유저 생성 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 중복 학번",
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
    ResponseEntity<ResponseDto<MemberResponseDto>> createMember(
            @Valid @RequestBody MemberCreateRequestDto request
    );

    @Operation(summary = "유저 전체 조회", description = "등록된 전체 유저 목록을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "전체 유저 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "studentNumber": "20240001",
                                  "nickName": "홍길동",
                                  "department": "COMPUTER_ENGINEERING",
                                  "email": "student@example.com",
                                  "lastLoginAt": "2026-08-18T12:00:00",
                                  "isNew": false
                                }
                              ],
                              "code": null,
                              "message": "전체 유저 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<MemberResponseDto>>> getMemberAll();

    @Operation(summary = "유저 단일 조회", description = "유저 ID로 단일 유저 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 조회 성공",
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
                                        "lastLoginAt": "2026-08-18T12:00:00",
                                        "isNew": false
                                      },
                                      "code": null,
                                      "message": "유저 조회 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 유저",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "MEMBER_NOT_FOUND",
                                      "message": "존재하지 않는 유저입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> getMember(
            @PathVariable Long memberId
    );

    @Operation(
            summary = "내 프로필 수정",
            description = "인증된 유저 본인의 닉네임, 학과, 이메일을 수정합니다. (관리자가 다른 유저를 수정하는 API가 아니라, 로그인한 본인 계정에만 적용됩니다)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 프로필 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "studentNumber": "20240001",
                                        "nickName": "새닉네임",
                                        "department": "COMPUTER_ENGINEERING",
                                        "email": "new-email@example.com",
                                        "lastLoginAt": "2026-08-18T12:00:00",
                                        "isNew": false
                                      },
                                      "code": null,
                                      "message": "유저 프로필 수정 성공"
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
                    description = "존재하지 않는 유저",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "MEMBER_NOT_FOUND",
                                      "message": "존재하지 않는 유저입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> updateMember(
            @AuthenticationPrincipal Member member,
            @RequestBody MemberUpdateRequestDto request
    );

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    인증된 유저 본인 계정을 탈퇴(삭제)합니다. 관리자가 다른 유저를 삭제하는 API가 아니라,
                    로그인한 본인 계정만 탈퇴할 수 있습니다. 응답 data는 탈퇴한 유저의 ID입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "유저 삭제 성공"
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
                    description = "존재하지 않는 유저",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "MEMBER_NOT_FOUND",
                                      "message": "존재하지 않는 유저입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteMember(
            @AuthenticationPrincipal Member member
    );

    @Operation(
            summary = "내 정보 조회",
            description = """
                    인증된 유저 본인의 정보를 조회합니다.
                    userType이 RESEARCHER인 경우 연동된 연구실, 커피챗, 연구실 리뷰 정보가 함께 채워집니다.
                    userType이 PROFESSOR인 경우 연동된 교수 정보(professor)와 교수 명의 연구실(laboratory)이 채워지며,
                    아직 교수 레코드와 연동되지 않았으면 professor/laboratory가, 연구실을 개설하지 않았으면 laboratory가 null로 내려갑니다.
                    그 외에는 laboratory/coffeeChat/labReview/professor가 모두 null로 내려갑니다.
                    bookmark(내 관심 연구실 목록)는 userType과 관계없이 항상 내려가며, 북마크가 없으면 빈 배열입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "연구자",
                                            value = """
                                                    {
                                                      "data": {
                                                        "id": 1,
                                                        "studentNumber": "20240001",
                                                        "nickName": "홍길동",
                                                        "department": "COMPUTER_ENGINEERING",
                                                        "email": "student@example.com",
                                                        "lastLoginAt": "2026-08-18T12:00:00",
                                                        "isNew": false,
                                                        "userType": "RESEARCHER",
                                                        "laboratory": {
                                                          "id": 1,
                                                          "labName": "소프트웨어공학 연구실"
                                                        },
                                                        "coffeeChat": {
                                                          "id": 1,
                                                          "laboratoryId": 1,
                                                          "laboratoryName": "소프트웨어공학 연구실",
                                                          "researcherId": 1,
                                                          "contactType": "KAKAO_OPEN_CHAT",
                                                          "contactValue": "https://open.kakao.com/o/example"
                                                        },
                                                        "labReview": {
                                                          "id": 1,
                                                          "laboratoryId": 1,
                                                          "coreTime": "있음",
                                                          "weeklyMeeting": "주 1회",
                                                          "doings": ["논문 리딩", "실험/코딩"]
                                                        },
                                                        "professor": null,
                                                        "bookmark": [
                                                          {
                                                            "id": 1,
                                                            "laboratory": {
                                                              "id": 2,
                                                              "labName": "지능형 데이터 시스템 연구실"
                                                            }
                                                          }
                                                        ]
                                                      },
                                                      "code": null,
                                                      "message": "내 정보 조회 성공"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "연구자가 아닌 경우",
                                            value = """
                                                    {
                                                      "data": {
                                                        "id": 2,
                                                        "studentNumber": "20240002",
                                                        "nickName": "이순신",
                                                        "department": "COMPUTER_ENGINEERING",
                                                        "email": "student2@example.com",
                                                        "lastLoginAt": "2026-08-18T12:00:00",
                                                        "isNew": false,
                                                        "userType": "FINDER",
                                                        "laboratory": null,
                                                        "coffeeChat": null,
                                                        "labReview": null,
                                                        "professor": null,
                                                        "bookmark": [
                                                          {
                                                            "id": 1,
                                                            "laboratory": {
                                                              "id": 2,
                                                              "labName": "지능형 데이터 시스템 연구실"
                                                            }
                                                          }
                                                        ]
                                                      },
                                                      "code": null,
                                                      "message": "내 정보 조회 성공"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "교수",
                                            value = """
                                                    {
                                                      "data": {
                                                        "id": 3,
                                                        "studentNumber": "12345678",
                                                        "nickName": "홍길동",
                                                        "department": "COMPUTER_ENGINEERING",
                                                        "email": "hong@inu.ac.kr",
                                                        "lastLoginAt": "2026-08-18T12:00:00",
                                                        "isNew": false,
                                                        "userType": "PROFESSOR",
                                                        "laboratory": {
                                                          "id": 1,
                                                          "labName": "소프트웨어공학 연구실"
                                                        },
                                                        "coffeeChat": null,
                                                        "labReview": null,
                                                        "professor": {
                                                          "id": 10,
                                                          "positionRaw": "교수",
                                                          "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                                          "collegeName": "정보기술대학",
                                                          "department": "COMPUTER_ENGINEERING",
                                                          "departmentName": "컴퓨터공학부",
                                                          "name": "홍길동",
                                                          "phoneNumber": "032-000-0000",
                                                          "email": "hong@inu.ac.kr"
                                                        },
                                                        "bookmark": []
                                                      },
                                                      "code": null,
                                                      "message": "내 정보 조회 성공"
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
            )
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> getMyInfo(
            @AuthenticationPrincipal Member member
    );
}
