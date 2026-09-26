package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorLinkRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "교수", description = "교수 계정 연동 관리 API (관리자 전용)")
public interface ProfessorApiSpecification {

    @Operation(
            summary = "교수-계정 연동",
            description = """
                    실제 교수님이 가입한 계정(memberId)을 엑셀로 미리 적재된 Professor 레코드(professorId)와 연결합니다.
                    본인 확인은 관리자가 별도 채널(전화, 학교 디렉터리 등)로 직접 수행한 뒤 이 API를 호출하는 것을 전제로 하며,
                    그래서 관리자(ROLE_ADMIN)만 호출할 수 있습니다.
                    이미 연동된 교수, 이미 다른 교수와 연동된 계정에는 사용할 수 없습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "연동할 교수 ID와 회원 ID",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProfessorLinkRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "professorId": 1,
                              "memberId": 42
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
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
                                      "code": null,
                                      "message": "교수 계정 연동 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 연동된 교수 또는 이미 다른 교수와 연동된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "이미 연동된 교수",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "PROFESSOR_ALREADY_LINKED",
                                                      "message": "이미 다른 계정과 연동된 교수입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "이미 연동된 계정",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "MEMBER_ALREADY_LINKED_TO_PROFESSOR",
                                                      "message": "이미 다른 교수와 연동된 계정입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 교수 또는 회원",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 교수",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "PROFESSOR_NOT_FOUND",
                                                      "message": "존재하지 않는 교수입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "존재하지 않는 회원",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "MEMBER_NOT_FOUND",
                                                      "message": "존재하지 않는 유저입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<ResponseDto<ProfessorResponseDto>> linkMember(
            @Valid @RequestBody ProfessorLinkRequestDto request
    );
}
