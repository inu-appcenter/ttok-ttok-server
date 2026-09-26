package com.inuappcenter.team_2_project_server.global.error.ex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "존재하지 않는 유저입니다."),

    // SSO 관련
    ORACLE_AUTH_UNAVAILABLE(HttpStatus.FORBIDDEN, "ORACLE_AUTH_UNAVAILABLE", "Oracle 연동을 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "학번 또는 비밀번호가 올바르지 않습니다."),

    // JWT 관련
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "TOKEN_MISSING", "인증 토큰이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "유효하지 않은 토큰입니다."),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "로그아웃되어 더 이상 사용할 수 없는 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),

    RESEARCH_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RESEARCH_KEYWORD_NOT_FOUND", "검색 키워드가 존재하지 않습니다."),
    EMPTY_EXCEL_FILE(HttpStatus.BAD_REQUEST, "EMPTY_EXCEL_FILE", "빈 엑셀 파일입니다."),
    NOT_FILE_EXTENDER(HttpStatus.BAD_REQUEST, "NOT_FILE_EXTENDER", "지원하지 않는 파일 확장자입니다."),
    DUPLICATED_LABORATORY(HttpStatus.BAD_REQUEST, "DUPLICATED_LAB", "중복된 연구실입니다."),
    INVALID_EXCEL_FILE(HttpStatus.BAD_REQUEST, "INVALID_EXCEL_FILE", "올바른 엑셀 파일이 아닙니다."),
    INVALID_EXCEL_SHEET(HttpStatus.BAD_REQUEST, "INVALID_EXCEL_SHEET", "엑셀 시트 형식이 올바르지 않습니다."),
    INVALID_EXCEL_HEADER(HttpStatus.BAD_REQUEST, "INVALID_EXCEL_HEADER", "엑셀 헤더 형식이 올바르지 않습니다."),
    INVALID_EXCEL_ROW(HttpStatus.BAD_REQUEST, "INVALID_EXCEL_ROW", "엑셀 행 데이터가 올바르지 않습니다."),
    PROFESSOR_NOT_FOUND_IN_EXCEL(HttpStatus.BAD_REQUEST, "PROFESSOR_NOT_FOUND_IN_EXCEL", "연구실 시트의 교수를 교수정보 시트에서 찾을 수 없습니다."),
    PROFESSOR_NOT_FOUND(HttpStatus.BAD_REQUEST, "PROFESSOR_NOT_FOUND", "존재하지 않는 교수입니다."),
    DUPLICATED_PROFESSOR_IN_EXCEL(HttpStatus.BAD_REQUEST, "DUPLICATED_PROFESSOR_IN_EXCEL", "엑셀 시트에 동일한 교수가 존재합니다."),
    LABORATORY_NOT_FOUND(HttpStatus.NOT_FOUND, "LABORATORY_NOT_FOUND", "존재하지 않는 연구실입니다."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "NO_SEARCH_KEYWORD", "허용하지 않는 검색어입니다."),
    RESEARCHER_NOT_FOUND(HttpStatus.NOT_FOUND, "RESEARCHER_NOT_FOUND", "존재하지 않는 연구생입니다."),
    COFFEE_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "COFFEE_CHAT_NOT_FOUND", "존재하지 않는 커피챗입니다."),
    COFFEE_CHAT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "COFFEE_CHAT_ALREADY_EXISTS", "1개의 커피챗만 생성 가능합니다."),
    RESEARCHER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "RESEARCHER_ALREADY_EXISTS", "이미 존재하는 연구자입니다."),
    INVALID_RESEARCHER(HttpStatus.BAD_REQUEST, "INVALID_RESEARCHER", "인증된 연구자가 아닙니다."),
    LAB_REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "LAB_REVIEW_ALREADY_EXIST", "해당 연구자의 랩리뷰가 이미 존재합니다."),
    LAB_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "LAB_REVIEW_NOT_FOUND", "존재하지 않는 연구실 리뷰입니다."),
    ONBOARDING_ALREADY_DONE(HttpStatus.BAD_REQUEST, "ONBOARDING_ALREADY_DONE", "이미 온보딩을 완료했습니다."),
    BUG_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "BUG_REPORT_NOT_FOUND", "오류 제보가 존재하지 않습니다."),
    RESEARCH_AREA_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "RESEARCH_AREA_CATEGORY_NOT_FOUND", "해당 카테고리가 존재하지 않습니다."),
    INVALID_LAB_ACCESS(HttpStatus.FORBIDDEN, "INVALID_LAB_ACCESS", "해당 연구실에 접근 권한이 없습니다."),
    PROFESSOR_ALREADY_LINKED(HttpStatus.BAD_REQUEST, "PROFESSOR_ALREADY_LINKED", "이미 다른 계정과 연동된 교수입니다."),
    MEMBER_ALREADY_LINKED_TO_PROFESSOR(HttpStatus.BAD_REQUEST, "MEMBER_ALREADY_LINKED_TO_PROFESSOR", "이미 다른 교수와 연동된 계정입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
