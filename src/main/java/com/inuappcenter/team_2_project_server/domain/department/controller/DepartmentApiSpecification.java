package com.inuappcenter.team_2_project_server.domain.department.controller;

import com.inuappcenter.team_2_project_server.domain.department.dto.response.CollegeResponseDto;
import com.inuappcenter.team_2_project_server.domain.department.dto.response.DepartmentResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "단과대/학과", description = "단과대, 학과 목록 조회 API")
public interface DepartmentApiSpecification {

    @Operation(
            summary = "단과대 전체 조회",
            description = """
                    서버에 정의된 단과대 목록을 전부 조회합니다. 개수가 적은 참조 데이터라 페이징하지 않습니다.
                    college 값은 다른 API 요청 시 단과대 코드로 그대로 사용합니다.
                    COLLEGE_OF_NULL 은 단과대에 속하지 않는 학부(법학부, 동북아국제통상물류학부)를 위한 값입니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "단과대 전체 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                { "college": "COLLEGE_OF_HUMANITIES", "collegeName": "인문대학" },
                                { "college": "COLLEGE_OF_NATURAL_SCIENCES", "collegeName": "자연과학대학" }
                              ],
                              "code": null,
                              "message": "단과대 전체 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<CollegeResponseDto>>> getAllColleges();

    @Operation(
            summary = "학과 전체 조회",
            description = """
                    서버에 정의된 학과 목록을 소속 단과대 정보와 함께 전부 조회합니다. 개수가 적은 참조 데이터라 페이징하지 않습니다.
                    department 값은 온보딩 등 다른 API 요청 시 학과 코드로 그대로 사용합니다. (예: professorDepartment)
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "학과 전체 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": [
                                {
                                  "department": "KOREAN",
                                  "departmentName": "국어국문학과",
                                  "college": "COLLEGE_OF_HUMANITIES",
                                  "collegeName": "인문대학"
                                },
                                {
                                  "department": "ENGLISH",
                                  "departmentName": "영어영문학과",
                                  "college": "COLLEGE_OF_HUMANITIES",
                                  "collegeName": "인문대학"
                                }
                              ],
                              "code": null,
                              "message": "학과 전체 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<List<DepartmentResponseDto>>> getAllDepartments();
}
