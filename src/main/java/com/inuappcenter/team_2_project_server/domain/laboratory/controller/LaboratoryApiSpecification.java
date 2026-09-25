package com.inuappcenter.team_2_project_server.domain.laboratory.controller;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.request.LaboratoryUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "연구실", description = "연구실 관리 및 편람 엑셀 동기화 API")
public interface LaboratoryApiSpecification {

    @Operation(
            summary = "연구실 편람 엑셀 import",
            description = """
                    연구실 편람 엑셀 파일을 업로드하여 교수, 연구실, 연구분야 키워드를 저장합니다.
                    연구실 시트와 교수정보 시트를 포함한 .xlsx 파일만 지원합니다.
                    이미 저장된 연구실은 중복 저장하지 않고 건너뜁니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 편람 동기화 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": null,
                                      "message": "연구실 편람 동기화 완료"
                                    }
                                    """)
                    )
            ),
    })
    ResponseEntity<ResponseDto<Void>> importLaboratory(
            @Parameter(
                    description = "연구실 편람 .xlsx 파일",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart MultipartFile file
    );

    @Operation(summary = "연구실 생성", description = "교수 ID를 기준으로 연구실을 수동 생성합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "생성할 연구실 정보. 연구분야는 쉼표 문자열이 아니라 배열로 전달합니다.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LaboratoryCreateRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                              "department": "COMPUTER_ENGINEERING",
                              "labName": "소프트웨어공학 연구실",
                              "location": "7호관 401호",
                              "capacity": {
                                "graduateStudentCount": 6,
                                "undergraduateStudentCount": 7
                              },
                              "introduction": "소프트웨어 품질과 개발 프로세스를 연구합니다.",
                              "professorId": 1,
                              "labUrl": "https://example.com/lab",
                              "researchAreas": [
                                "소프트웨어공학",
                                "인공지능"
                              ]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
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
                                        "researchAreas": [
                                          "소프트웨어공학",
                                          "인공지능"
                                        ]
                                      },
                                      "code": null,
                                      "message": "연구실 생성 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 교수 또는 중복 연구실",
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
                                            name = "중복 연구실",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "code": "DUPLICATED_LAB",
                                                      "message": "중복된 연구실입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<ResponseDto<LaboratoryResponseDto>> createLaboratory(
            @RequestBody LaboratoryCreateRequestDto request
    );

    @Operation(summary = "연구실 단건 조회", description = "연구실 ID로 단일 연구실 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
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
                                        "researchAreas": [
                                          "소프트웨어공학",
                                          "인공지능"
                                        ]
                                      },
                                      "code": null,
                                      "message": "연구실 조회 성공"
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
    ResponseEntity<ResponseDto<LaboratoryResponseDto>> getLaboratory(
            @PathVariable Long laboratoryId
    );

    @Operation(
            summary = "연구실 전체 조회",
            description = """
                    등록된 연구실 목록을 페이지 단위로 조회합니다.
                    page(0-based), size, sort(예: `labName,asc` / `labName,desc`) 쿼리 파라미터를 사용합니다.
                    기본값은 size=20, labName 오름차순입니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "전체 연구실 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": {
                                "content": [
                                  {
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
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 137,
                                "totalPages": 7,
                                "hasNext": true,
                                "last": false
                              },
                              "code": null,
                              "message": "전체 연구실 조회 성공"
                            }
                            """)
            )
    )
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "0부터 시작하는 페이지 번호", example = "0")
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "한 페이지에 담을 개수 (기본값 20)", example = "20")
    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "정렬 조건. `필드명,asc|desc` 형식 (기본값 labName,asc)", example = "labName,asc")
    ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> getAllLaboratory(
            @ParameterObject
            @PageableDefault(size = 20, sort = "labName", direction = Sort.Direction.ASC) Pageable pageable
    );

    @Operation(
            summary = "연구실 수정",
            description = """
                    연구실 ID로 연구실 정보를 수정합니다.
                    요청에 포함된 값만 수정하며, 연구분야는 쉼표 문자열이 아니라 배열로 전달합니다.
                    인증된 사용자가 해당 연구실 소속(연구자)인 경우에만 수정할 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 연구실 정보. 변경하지 않을 필드는 요청에서 제외합니다.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LaboratoryUpdateRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "location": "7호관 402호",
                              "capacity": {
                                "undergraduateStudentCount": 8
                              },
                              "researchAreas": [
                                "소프트웨어공학",
                                "데이터마이닝"
                              ]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "id": 1,
                                        "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                        "collegeName": "정보기술대학",
                                        "department": "COMPUTER_ENGINEERING",
                                        "departmentName": "컴퓨터공학부",
                                        "labName": "소프트웨어공학 연구실",
                                        "location": "7호관 402호",
                                        "capacity": {
                                          "graduateStudentCount": 6,
                                          "undergraduateStudentCount": 8
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
                                        "researchAreas": [
                                          "소프트웨어공학",
                                          "데이터마이닝"
                                        ]
                                      },
                                      "code": null,
                                      "message": "연구실 수정 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 연구실 소속이 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "INVALID_LAB_ACCESS",
                                      "message": "해당 연구실에 접근 권한이 없습니다."
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
    ResponseEntity<ResponseDto<LaboratoryResponseDto>> updateLaboratory(
            @AuthenticationPrincipal Member member,
            @PathVariable Long laboratoryId,
            @RequestBody LaboratoryUpdateRequestDto request
    );

    @Operation(
            summary = "연구실 삭제",
            description = """
                    연구실 ID로 연구실을 삭제합니다.
                    인증된 사용자가 해당 연구실 소속(연구자)인 경우에만 삭제할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": 1,
                                      "code": null,
                                      "message": "연구실 삭제 완료"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 연구실 소속이 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "INVALID_LAB_ACCESS",
                                      "message": "해당 연구실에 접근 권한이 없습니다."
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
    ResponseEntity<ResponseDto<Long>> deleteLaboratory(
            @AuthenticationPrincipal Member member,
            @PathVariable Long laboratoryId
    );

    @Operation(
            summary = "연구실 검색",
            description = """
                    연구실명 또는 교수명에 검색어가 포함된 연구실을 페이지 단위로 조회합니다.
                    page(0-based), size, sort 쿼리 파라미터를 사용하며 기본값은 size=20 입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "content": [
                                          {
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
                                        ],
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 3,
                                        "totalPages": 1,
                                        "hasNext": false,
                                        "last": true
                                      },
                                      "code": null,
                                      "message": "연구실 검색 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 비어 있음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "NO_SEARCH_KEYWORD",
                                      "message": "허용하지 않는 검색어입니다."
                                    }
                                    """)
                    )
            )
    })
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "0부터 시작하는 페이지 번호", example = "0")
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "한 페이지에 담을 개수 (기본값 20)", example = "20")
    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "정렬 조건. `필드명,asc|desc` 형식 (예: labName,desc)", example = "labName,asc")
    ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> searchLaboratory(
            @Parameter(description = "연구실명 또는 교수명 검색어", required = true, example = "홍길동")
            @RequestParam String keyword,
            @ParameterObject
            @PageableDefault(size = 20) Pageable pageable
    );

    @Operation(
            summary = "카테고리별 연구실 검색",
            description = """
                    상위 연구분야 카테고리명으로 검색하면, 그 카테고리에 속한 모든 하위 연구분야의 연구실을 페이지 단위로 조회합니다.
                    page(0-based) 쿼리 파라미터만 사용하며, 한 페이지당 20건으로 고정입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카테고리별 연구실 검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "content": [
                                          {
                                            "id": 1,
                                            "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                            "collegeName": "정보기술대학",
                                            "department": "COMPUTER_ENGINEERING",
                                            "departmentName": "컴퓨터공학부",
                                            "labName": "지능제어 및 기계학습 LAB",
                                            "location": "7호관 401호",
                                            "capacity": {
                                              "graduateStudentCount": 6,
                                              "undergraduateStudentCount": 7
                                            },
                                            "introduction": "강화학습과 머신러닝을 연구합니다.",
                                            "professor": {
                                              "id": 1,
                                              "positionRaw": "교수",
                                              "college": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                              "collegeName": "정보기술대학",
                                              "department": "COMPUTER_ENGINEERING",
                                              "departmentName": "컴퓨터공학부",
                                              "name": "이명훈",
                                              "phoneNumber": "032-835-0000",
                                              "email": "professor@example.com"
                                            },
                                            "labUrl": "https://example.com/lab",
                                            "researchAreas": ["강화학습 및 머신러닝"]
                                          }
                                        ],
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 4,
                                        "totalPages": 1,
                                        "hasNext": false,
                                        "last": true
                                      },
                                      "code": null,
                                      "message": "카테고리별 연구실 검색 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "카테고리명이 비어 있음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "NO_SEARCH_KEYWORD",
                                      "message": "허용하지 않는 검색어입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<PageResponseDto<LaboratoryResponseDto>>> searchLaboratoryByCategory(
            @Parameter(description = "상위 연구분야 카테고리명", required = true, example = "AI")
            @RequestParam String categoryName,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page
    );

    @Operation(
            summary = "연구실 논문 목록 조회",
            description = """
                    연구실 ID로 그 연구실의 논문 목록을 페이지 단위로 조회합니다.
                    page(0-based) 쿼리 파라미터만 사용하며, 한 페이지당 5건으로 고정이고 연도 내림차순으로 정렬됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 논문 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "content": [
                                          {
                                            "id": 1,
                                            "title": "딥러닝 기반 이상 탐지 기법 연구",
                                            "researchersRaw": "홍길동, 이순신",
                                            "platform": "IEEE",
                                            "year": "2024",
                                            "type": "JOURNAL",
                                            "status": "PUBLISHED",
                                            "doi": "10.1000/example",
                                            "sourceURL": "https://doi.org/10.1000/example"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 5,
                                        "totalElements": 12,
                                        "totalPages": 3,
                                        "hasNext": true,
                                        "last": false
                                      },
                                      "code": null,
                                      "message": "연구실 논문 목록 조회 성공"
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
    ResponseEntity<ResponseDto<PageResponseDto<PublicationResponseDto>>> getPublications(
            @PathVariable Long laboratoryId,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page
    );
}
