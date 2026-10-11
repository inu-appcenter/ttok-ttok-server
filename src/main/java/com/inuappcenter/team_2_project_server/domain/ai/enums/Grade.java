package com.inuappcenter.team_2_project_server.domain.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메일 쓰기 화면의 학년 칩
 * - description: 화면 표시용
 * - introText: 메일 자기소개 문장("저는 ○○학과 3학년 홍길동입니다")에 넣는 값
 */
@Getter
@RequiredArgsConstructor
public enum Grade {
    FIRST("1학년", "1학년"),
    SECOND("2학년", "2학년"),
    THIRD("3학년", "3학년"),
    FOURTH("4학년", "4학년"),
    FIFTHPLUS("5학년+", "5학년");

    private final String description;
    private final String introText;
}
