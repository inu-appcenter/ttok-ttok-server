package com.inuappcenter.team_2_project_server.domain.labReview.controller;

import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewOptionsResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewResponseDto;
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

import java.util.List;

@Tag(name = "연구실 리뷰", description = "학부연구생이 작성하는 연구실 생활 정보(코어타임/주간 미팅/하는 일) API")
public interface LabReviewApiSpecification {

    @Operation(
            summary = "연구실 리뷰 작성 폼 선택지 조회",
            description = """
                    리뷰 작성 폼에 표시할 코어타임 / 주간 미팅 / 하는 일 기본 선택지 목록입니다.
                    연구생은 이 목록에 없는 값도 자유 입력할 수 있습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "연구실 리뷰 선택지 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": {
                                "coreTime": ["있음", "없음", "자율"],
                                "weeklyMeeting": ["주 1회", "주 2회 이상", "격주", "월 1회", "비정기", "없음"],
                                "works": [
                                  "논문 리딩", "실험/코딩", "데이터 라벨링", "데이터 수집/전처리",
                                  "논문 작성", "프로젝트 개발", "세미나 발표", "코드 리뷰",
                                  "대회/공모전 참가", "장비/서버 관리", "학회 참석", "연구과제",
                                  "개인연구", "1:1미팅", "랩미팅"
                                ]
                              },
                              "code": null,
                              "message": "연구실 리뷰 선택지 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<LabReviewOptionsResponseDto>> getOptions();

    @Operation(
            summary = "연구실 리뷰 작성",
            description = """
                    인증된 연구생이 본인 소속 연구실에 대한 리뷰를 작성합니다.
                    연구실은 토큰에서 식별한 연구생의 소속으로 자동 지정되며, 연구생당 1개만 작성할 수 있습니다.
                    coreTime / weeklyMeeting / doings 는 기본 옵션 외 자유 입력도 가능하며 저장 시 공백이 정리됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "코어타임, 주간 미팅, 하는 일",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabReviewRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "coreTime": "있음",
                              "weeklyMeeting": "주 1회",
                              "doings": ["논문 리딩", "실험/코딩"]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 리뷰 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "laboratoryId": 465,
                                        "coreTime": "있음",
                                        "weeklyMeeting": "주 1회",
                                        "doings": ["논문 리딩", "실험/코딩"]
                                      },
                                      "code": null,
                                      "message": "연구실 리뷰 생성 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 이미 작성한 리뷰가 존재",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "LAB_REVIEW_ALREADY_EXISTS",
                                      "message": "해당 연구자의 랩리뷰가 이미 존재합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "연구생이 아니거나 소속 연구실이 없음",
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
    ResponseEntity<ResponseDto<LabReviewResponseDto>> submit(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody LabReviewRequestDto request
    );

    @Operation(
            summary = "연구실 리뷰 목록 조회",
            description = "연구실 ID로 해당 연구실에 작성된 리뷰 목록을 조회합니다. 응답에는 작성자 정보가 포함되지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "연구실 리뷰 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "laboratoryId": 465,
                                  "coreTime": "있음",
                                  "weeklyMeeting": "주 1회",
                                  "doings": ["논문 리딩", "실험/코딩"]
                                },
                                {
                                  "id": 2,
                                  "laboratoryId": 465,
                                  "coreTime": "없음",
                                  "weeklyMeeting": "격주",
                                  "doings": ["논문 리딩", "데이터 라벨링"]
                                }
                              ],
                              "code": null,
                              "message": "연구실 리뷰 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<LabReviewResponseDto>>> getLabReviews(
            @Parameter(description = "리뷰를 조회할 연구실 ID", required = true, example = "465")
            @RequestParam Long laboratoryId
    );

    @Operation(
            summary = "내 연구실 리뷰 조회",
            description = "인증된 연구생이 작성한 리뷰를 조회합니다. 작성한 리뷰가 없으면 data 는 null 입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "내 연구실 리뷰 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": {
                                "id": 1,
                                "laboratoryId": 465,
                                "coreTime": "있음",
                                "weeklyMeeting": "주 1회",
                                "doings": ["논문 리딩", "실험/코딩"]
                              },
                              "code": null,
                              "message": "내 연구실 리뷰 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<LabReviewResponseDto>> getMyLabReview(
            @AuthenticationPrincipal Member member
    );

    @Operation(
            summary = "내 연구실 리뷰 수정",
            description = "인증된 연구생이 작성한 리뷰의 코어타임, 주간 미팅, 하는 일을 수정합니다. 세 값 모두 전달해야 합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 코어타임, 주간 미팅, 하는 일",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabReviewRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "coreTime": "없음",
                              "weeklyMeeting": "격주",
                              "doings": ["논문 리딩", "데이터 라벨링", "세미나 발표"]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 연구실 리뷰 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "laboratoryId": 465,
                                        "coreTime": "없음",
                                        "weeklyMeeting": "격주",
                                        "doings": ["논문 리딩", "데이터 라벨링", "세미나 발표"]
                                      },
                                      "code": null,
                                      "message": "내 연구실 리뷰 수정 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "작성한 리뷰가 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "LAB_REVIEW_NOT_FOUND",
                                      "message": "존재하지 않는 연구실 리뷰입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabReviewResponseDto>> updateMyLabReview(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody LabReviewRequestDto request
    );

    @Operation(
            summary = "내 연구실 리뷰 삭제",
            description = "인증된 연구생이 작성한 리뷰를 삭제합니다. 응답 data 는 삭제된 리뷰 ID 입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 연구실 리뷰 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "내 연구실 리뷰 삭제 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "작성한 리뷰가 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "LAB_REVIEW_NOT_FOUND",
                                      "message": "존재하지 않는 연구실 리뷰입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteMyLabReview(
            @AuthenticationPrincipal Member member
    );
}
