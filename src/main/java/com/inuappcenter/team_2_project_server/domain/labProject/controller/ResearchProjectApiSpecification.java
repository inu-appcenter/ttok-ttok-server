package com.inuappcenter.team_2_project_server.domain.labProject.controller;

import com.inuappcenter.team_2_project_server.domain.labProject.dto.ResearchProjectAssignRequestDto;
import com.inuappcenter.team_2_project_server.domain.labProject.dto.ResearchProjectResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "연구과제", description = "연구실 연구과제 조회 및 NTIS 동기화 API")
public interface ResearchProjectApiSpecification {

    @Operation(
            summary = "연구실 연구과제 목록 조회",
            description = """
                    연구실 ID로 그 연구실의 국가R&D 연구과제 목록을 페이지 단위로 조회합니다.
                    NTIS(국가과학기술지식정보서비스) 국가R&D 과제검색 API에서 담당교수 이름으로 주기적으로 동기화해둔 값을 그대로 보여줍니다.
                    page(0-based) 쿼리 파라미터만 사용하며, 한 페이지당 5건으로 고정이고 진행중인 과제·최근 연도 순으로 정렬됩니다.
                    ntisDetailUrl은 NTIS 원본 상세페이지 링크이며, 열람하려면 이용자가 NTIS에 별도로 로그인해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 연구과제 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "content": [
                                          {
                                            "id": 1,
                                            "projectNumber": "1711041912",
                                            "titleKorean": "스마트 센서 응용을 위한 나노 멤브레인 공정 플랫폼 개발",
                                            "titleEnglish": "Platform development of nano membrane process for smart sensor application",
                                            "managerName": "이종근",
                                            "budgetProjectName": "나노·소재기술개발",
                                            "researchAgencyName": "인천대학교",
                                            "ministryName": "과학기술정보통신부",
                                            "projectYear": "2016",
                                            "periodStart": "20160701",
                                            "periodEnd": "20170630",
                                            "totalPeriodStart": "2015-10-01 00:00:00.0",
                                            "totalPeriodEnd": "2020-06-30 00:00:00.0",
                                            "governmentFunds": "300000000",
                                            "totalFunds": "475000000",
                                            "contentSummary": "나노 멤브레인 공정 플랫폼 개발 및 파운드리 서비스 제공을 위한 연구...",
                                            "keywordKorean": "나노 멤브레인,NEMS,가스 센서",
                                            "keywordEnglish": "Nanomembrane,NEMS,Gassensor",
                                            "ongoing": false,
                                            "ntisDetailUrl": "https://www.ntis.go.kr/project/pjtInfo.do?pjtId=1711041912"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 5,
                                        "totalElements": 3,
                                        "totalPages": 1,
                                        "hasNext": false,
                                        "last": true
                                      },
                                      "code": null,
                                      "message": "연구실 연구과제 목록 조회 성공"
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
    ResponseEntity<ResponseDto<PageResponseDto<ResearchProjectResponseDto>>> getResearchProjects(
            @PathVariable Long laboratoryId,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page
    );

    @Operation(
            summary = "전체 연구실 연구과제 수동 동기화 (관리자 전용)",
            description = """
                    등록된 모든 연구실에 대해 NTIS 국가R&D 과제검색 API를 담당교수 이름으로 즉시 조회하여
                    연구과제 데이터를 동기화합니다. 매일 새벽 자동 배치와 별개로, 지금 바로 반영이 필요할 때 사용합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "전체 동기화 완료",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": null,
                              "code": null,
                              "message": "연구과제 전체 동기화 완료"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<Void>> syncAllResearchProjects();

    @Operation(
            summary = "연구실 매핑 보류 연구과제 목록 조회 (관리자 전용)",
            description = """
                    동기화 배치가 어느 연구실 과제인지 판별하지 못해 연구실 매핑을 보류한 연구과제 목록을 조회합니다.
                    NTIS는 연구책임자 이름으로만 검색되어, 같은 이름의 교수(동명이인)가 있으면 과제 분류로 판별하고
                    판별이 애매하면 보류합니다. 보류된 과제는 연구실 연구과제 목록에 노출되지 않습니다.
                    page(0-based), 한 페이지당 20건으로 고정이며 연구책임자 이름순으로 정렬됩니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "매핑 보류 연구과제 목록 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "data": {
                                "content": [
                                  {
                                    "id": 12,
                                    "projectNumber": "1711041912",
                                    "titleKorean": "차세대 모빌리티용 수소저장 시스템 개발",
                                    "titleEnglish": null,
                                    "managerName": "김태완",
                                    "budgetProjectName": "에너지기술수용성제고및사업화촉진(R&D)",
                                    "researchAgencyName": "인천대학교",
                                    "ministryName": "산업통상자원부",
                                    "projectYear": "2025",
                                    "periodStart": "20250101",
                                    "periodEnd": "20251231",
                                    "totalPeriodStart": "2024-04-01 00:00:00.0",
                                    "totalPeriodEnd": "2027-12-31 00:00:00.0",
                                    "governmentFunds": "100000000",
                                    "totalFunds": "150000000",
                                    "contentSummary": "수소저장 시스템 개발...",
                                    "keywordKorean": "수소저장,모빌리티",
                                    "keywordEnglish": "hydrogen storage,mobility",
                                    "ongoing": true,
                                    "ntisDetailUrl": "https://www.ntis.go.kr/project/pjtInfo.do?pjtId=1711041912"
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 1,
                                "totalPages": 1,
                                "hasNext": false,
                                "last": true
                              },
                              "code": null,
                              "message": "매핑 보류 연구과제 목록 조회 성공"
                            }
                            """)
            )
    )
    ResponseEntity<ResponseDto<PageResponseDto<ResearchProjectResponseDto>>> getPendingResearchProjects(
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page
    );

    @Operation(
            summary = "연구과제 연구실 수동 지정 (관리자 전용)",
            description = """
                    연구과제를 어느 연구실 과제로 보여줄지 관리자가 직접 지정합니다.
                    매핑이 보류됐거나 잘못 매핑된 과제를 바로잡을 때 사용하며, 이후 동기화 배치의 자동 매핑이 덮어쓰지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구과제 연구실 지정 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 연구과제 또는 연구실",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RESEARCH_PROJECT_NOT_FOUND",
                                      "message": "존재하지 않는 연구과제입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<ResearchProjectResponseDto>> assignResearchProjectLaboratory(
            @Parameter(description = "연구과제 ID", required = true, example = "12")
            @PathVariable Long researchProjectId,
            @RequestBody ResearchProjectAssignRequestDto request
    );
}
