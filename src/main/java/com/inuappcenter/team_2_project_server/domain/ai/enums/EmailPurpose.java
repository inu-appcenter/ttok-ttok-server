package com.inuappcenter.team_2_project_server.domain.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메일 쓰기 목적
 * - description: 화면 표시용, LLM 프롬프트의 [메일 목적]에도 쓴다
 * - introSentence: 자기소개 뒤에 붙는 연락 목적 문장 (서버가 고정으로 붙인다)
 * - subjectLabel: 메일 제목 앞머리 "[학부연구생 지원 문의] 국어국문학과 홍길동"
 */
@Getter
@RequiredArgsConstructor
public enum EmailPurpose {
    UNDERGRADUATE_RESEARCH(
            "학부연구생 지원",
            "교수님 연구실의 학부연구생으로 지원하고 싶어 연락드렸습니다.",
            "학부연구생 지원 문의"
    ),
    GRADUATE_CONSULTING(
            "대학원 진학 상담",
            "교수님 연구실로의 대학원 진학에 대해 상담드리고 싶어 연락드렸습니다.",
            "대학원 진학 상담 문의"
    ),
    QUESTION(
            "교수님께 질문",
            "교수님의 연구와 관련해 여쭙고 싶은 점이 있어 연락드렸습니다.",
            "질문 문의"
    );

    private final String description;
    private final String introSentence;
    private final String subjectLabel;
}
