package com.inuappcenter.team_2_project_server.domain.ai.dto.response;

/**
 * 교수님께 메일 쓰기 응답
 * - subject: 메일 제목 "[학부연구생 지원 문의] 국어국문학과 홍길동"
 * - body: 인사말 + 자기소개 + 연락 목적(서버 고정) / 본문(AI 생성) / 끝인사 + 서명(서버 고정)
 */
public record EmailResponseDto(
        String subject,
        String body
) {
}
