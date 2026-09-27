package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "교수", description = "교수 본인 정보 수정 API")
public interface ProfessorApiSpecification {

    @Operation(
            summary = "교수 본인 정보 수정",
            description = """
                    로그인한 계정과 연동된 Professor 레코드를 찾아, 요청에 포함된 값(직급/전화번호/이메일)만 수정합니다.
                    학과·이름은 유니크 제약(department, name, email) 및 엑셀 재매칭 키와 얽혀있어 이 API로 수정할 수 없습니다.
                    아직 계정 연동 기능(이메일 인증 등)이 없어, 지금은 어떤 로그인 계정도 Professor와 연동되어 있지 않습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 값. 보내지 않은 필드는 그대로 유지됩니다.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProfessorUpdateRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "positionRaw": "부교수",
                              "phoneNumber": "032-835-1234",
                              "email": "professor@inu.ac.kr"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "positionRaw": "부교수",
                                        "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                        "collegeName": "정보기술대학",
                                        "department": "COMPUTER_ENGINEERING",
                                        "departmentName": "컴퓨터공학부",
                                        "name": "홍길동",
                                        "phoneNumber": "032-835-1234",
                                        "email": "professor@inu.ac.kr"
                                      },
                                      "code": null,
                                      "message": "교수 정보 수정 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "로그인 계정과 연동된 교수 레코드가 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "PROFESSOR_NOT_FOUND",
                                      "message": "존재하지 않는 교수입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<ProfessorResponseDto>> updateMyProfile(
            @AuthenticationPrincipal Member member,
            @RequestBody ProfessorUpdateRequestDto request
    );
}
