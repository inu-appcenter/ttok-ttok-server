package com.inuappcenter.team_2_project_server.domain.bookmark.controller;

import com.inuappcenter.team_2_project_server.domain.bookmark.dto.BookmarkResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "북마크", description = "연구실 북마크 API")
public interface BookmarkApiSpecification {

    @Operation(
            summary = "내 북마크 목록 조회",
            description = "인증된 유저 본인이 북마크한 연구실 목록을 전부 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "내 북마크 목록 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                {
                                  "id": 1,
                                  "laboratory": {
                                    "id": 1,
                                    "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                    "collegeName": "정보기술대학",
                                    "department": "COMPUTER_ENGINEERING",
                                    "departmentName": "컴퓨터공학부",
                                    "labName": "소프트웨어공학 연구실",
                                    "location": "7호관 401호",
                                    "capacity": {
                                      "graduateStudentCount": 6,
                                      "undergraduateStudentCount": 7
                                    },
                                    "introduction": "소프트웨어 품질과 개발 프로세스를 연구합니다.",
                                    "professor": {
                                      "id": 1,
                                      "positionRaw": "교수",
                                      "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                      "collegeName": "정보기술대학",
                                      "department": "COMPUTER_ENGINEERING",
                                      "departmentName": "컴퓨터공학부",
                                      "name": "홍길동",
                                      "phoneNumber": "032-835-0000",
                                      "email": "professor@example.com"
                                    },
                                    "labUrl": "https://example.com/lab",
                                    "researchAreas": ["소프트웨어공학", "인공지능"]
                                  }
                                }
                              ],
                              "code": null,
                              "message": "내 북마크 목록 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<BookmarkResponseDto>>> getMyBookmark(
            @AuthenticationPrincipal Member member
    );

    @Operation(
            summary = "연구실 북마크 토글",
            description = """
                    선택한 연구실에 대한 북마크를 토글합니다.
                    이미 북마크한 연구실이면 해제(삭제)하고, 아니면 새로 등록합니다.
                    응답 data는 토글 대상 연구실 ID입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "북마크 토글 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "선택한 연구실 북마크 토글 성공"
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
    ResponseEntity<ResponseDto<Long>> toggleBookmark(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "북마크를 토글할 연구실 ID", required = true, example = "1")
            @RequestParam Long laboratoryId
    );
}
