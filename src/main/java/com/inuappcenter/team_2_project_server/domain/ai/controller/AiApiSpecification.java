package com.inuappcenter.team_2_project_server.domain.ai.controller;

import com.inuappcenter.team_2_project_server.domain.ai.dto.request.EmailRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabKeywordSuggestionRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.EmailResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabKeywordSuggestionResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI", description = "연구실 추천(RAG)과 교수님께 메일 쓰기 API")
public interface AiApiSpecification {

    @Operation(
            summary = "연구실 추천",
            description = """
                    학생의 관심사(interest)와 고른 키워드(selectedKeywords)에 맞는 연구실을 최대 3개 추천합니다. 로그인한 유저만 사용할 수 있습니다.
                    - 연구실 소개와 논문을 검색해 후보를 찾고(RAG), AI가 후보 중에서 요청에 맞는 연구실을 골라 추천 이유를 씁니다.
                    - 확실히 맞는 연구실만 고르므로 3개보다 적을 수 있습니다. 첫 번째가 가장 잘 맞는 연구실입니다.
                    - 맞는 연구실이 없으면 noMatch가 true이고 recommendations는 빈 목록입니다.
                    - keywords는 연구실 연구분야 태그, reason은 한 줄짜리 추천 이유입니다.
                    - AI 선택 단계가 실패하면 검색 순위 상위 3개를 돌려주며, 이때 reason은 null입니다.
                    - laboratoryId로 연구실 상세 화면으로 이동할 수 있습니다.
                    - interest는 비어 있으면 안 되고 최대 300자입니다.
                    - interest는 키워드 추천 API(POST /api/chat/research-lab/keywords)에 보낸 것과 같은 값을 보냅니다.
                    - selectedKeywords는 키워드 추천 API로 받은 키워드 중 고른 것과 직접 입력한 키워드입니다.
                      최대 3개, 각 최대 30자이며 생략하거나 빈 목록이어도 됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "학생의 관심사와 고른 키워드",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabRecommendationRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "interest": "물을 전기분해해서 그린수소 만드는 촉매를 연구하고 싶어요",
                              "selectedKeywords": ["에너지", "촉매"]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 추천 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "noMatch": false,
                                        "recommendations": [
                                          {
                                            "laboratoryId": 157,
                                            "labName": "무기나노촉매 연구실",
                                            "department": "화학과",
                                            "professorName": "권태현",
                                            "keywords": ["나노촉매", "수전해", "에너지 소재"],
                                            "reason": "나노촉매로 수전해 수소 생산 효율을 높이는 연구를 해요"
                                          }
                                        ]
                                      },
                                      "code": null,
                                      "message": "연구실 추천 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_MISSING",
                                      "message": "인증 토큰이 필요합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 장애, 검색 문서 미생성 등으로 추천 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RECOMMENDATION_UNAVAILABLE",
                                      "message": "연구실 추천을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabRecommendationResponseDto>> researchLabChat(@RequestBody LabRecommendationRequestDto request);

    @Operation(
            summary = "연구실 추천 키워드",
            description = """
                    학생이 자유롭게 적은 관심사(interest)를 보고, 고를 만한 연구분야 키워드를 최대 5개 돌려줍니다. 로그인한 유저만 사용할 수 있습니다.
                    - 관심사와 가까운 연구실들을 찾아, 그 연구실들이 가진 연구분야 카테고리를 많이 나온 순으로 돌려줍니다.
                    - 실제 연구실이 가진 카테고리만 나오므로, 어떤 키워드를 골라도 추천할 연구실이 있습니다.
                    - 맞는 연구실을 찾지 못하면 keywords는 빈 목록입니다. 이때는 직접 입력으로 진행하면 됩니다.
                    - 연구실 추천 API(POST /api/chat/research-lab)를 호출할 때 같은 interest와, 고른 키워드를 selectedKeywords로 보냅니다.
                    - interest는 비어 있으면 안 되고 최대 300자입니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "학생의 관심사",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LabKeywordSuggestionRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "interest": "추천시스템에 관심이 있고, 파이썬으로 크롤링 프로젝트를 해봤어요."
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연구실 추천 키워드 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "keywords": ["AI", "데이터", "웹", "알고리즘", "소프트웨어공학"]
                                      },
                                      "code": null,
                                      "message": "연구실 추천 키워드 조회 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_MISSING",
                                      "message": "인증 토큰이 필요합니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스(임베딩) 장애로 키워드 추천 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "RECOMMENDATION_UNAVAILABLE",
                                      "message": "연구실 추천을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabKeywordSuggestionResponseDto>> suggestResearchLabKeywords(@RequestBody LabKeywordSuggestionRequestDto request);

    @Operation(
            summary = "연구실 추천 검색 문서 수동 동기화 (관리자 전용)",
            description = """
                    연구실 소개와 논문을 임베딩해 추천 검색 문서를 갱신합니다. 매일 새벽 5시 자동 배치와 별개로 지금 바로 반영할 때 사용합니다.
                    - 내용이 바뀐 연구실/논문만 다시 임베딩하고, 삭제된 연구실/논문의 문서는 지웁니다.
                    - 엑셀로 연구실 데이터를 새로 가져온 뒤나, 처음 배포한 뒤에 실행해야 추천이 동작합니다.
                    - 처음 실행할 때는 전체를 임베딩하므로 몇 분 걸립니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "동기화 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "created": 12,
                                        "updated": 3,
                                        "deleted": 1,
                                        "failed": 0,
                                        "unchanged": 10414
                                      },
                                      "code": null,
                                      "message": "연구실 추천 검색 문서 동기화 완료"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<LabSearchIndexSyncResponseDto>> syncResearchLabIndex();

    @Operation(
            summary = "교수님께 메일 쓰기 (초안 생성)",
            description = """
                    연구실 상세 화면에서 입력한 항목으로 교수님께 보낼 메일의 제목과 본문 초안을 만듭니다. 로그인한 유저만 사용할 수 있습니다.
                    - 연구실명, 연구 분야, 소개, 담당 교수 이름은 laboratoryId로 서버가 DB에서 채웁니다.
                    - 본문 형식: 인사말("안녕하세요, ○○○ 교수님.") + 자기소개와 연락 목적 문장 + AI가 쓴 본문 + "감사합니다. ○○○ 드림"
                      인사말, 자기소개, 연락 목적 문장, 끝인사, 서명은 서버가 고정 형식으로 붙이고, AI는 가운데 본문만 씁니다.
                    - 제목 형식: "[학부연구생 지원 문의] 국어국문학과 홍길동"
                    - purpose: UNDERGRADUATE_RESEARCH(학부연구생 지원), GRADUATE_CONSULTING(대학원 진학 상담), QUESTION(교수님께 질문)
                    - grade: FIRST, SECOND, THIRD, FOURTH, FIFTHPLUS(5학년+)
                    - email, phoneNumber는 받지만 초안에는 넣지 않습니다.
                    - 검증: name 최대 20자, email 형식, phoneNumber 최대 13자(선택), interest 필수 최대 300자, content 최대 1200자(선택)
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "메일 쓰기 입력 항목",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EmailRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "purpose": "UNDERGRADUATE_RESEARCH",
                              "laboratoryId": 157,
                              "name": "홍길동",
                              "department": "COMPUTER_ENGINEERING",
                              "grade": "THIRD",
                              "email": "ttokttok@gmail.com",
                              "phoneNumber": "010-1234-5678",
                              "interest": "나노촉매를 이용한 수전해 수소 생산 연구에 관심이 있습니다.",
                              "content": "일반화학실험 수업에서 촉매 반응 실험을 했고, 수소 에너지 관련 조별 과제를 진행했습니다."
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메일 초안 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "subject": "[학부연구생 지원 문의] 컴퓨터공학부 홍길동",
                                        "body": "안녕하세요, 권태현 교수님.\n저는 컴퓨터공학부 3학년 홍길동입니다. 교수님 연구실의 학부연구생으로 지원하고 싶어 연락드렸습니다.\n\n(AI가 쓴 본문)\n\n감사합니다.\n홍길동 드림"
                                      },
                                      "code": null,
                                      "message": "메일 초안 생성 성공"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "TOKEN_MISSING",
                                      "message": "인증 토큰이 필요합니다."
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
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 장애로 본문 생성 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": null,
                                      "code": "EMAIL_DRAFT_UNAVAILABLE",
                                      "message": "메일 초안을 일시적으로 만들 수 없습니다. 잠시 후 다시 시도해 주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ResponseDto<EmailResponseDto>> writeEmailDraft(@RequestBody EmailRequestDto request);
}
