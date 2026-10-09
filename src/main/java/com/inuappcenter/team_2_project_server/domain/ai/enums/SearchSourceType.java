package com.inuappcenter.team_2_project_server.domain.ai.enums;

/**
 * 연구실 추천 검색 문서의 원본 종류
 * - PROFILE: 연구실 프로필 (연구실명, 학과, 교수, 연구분야, 소개) -> source_id는 연구실 id
 * - PUBLICATION: 논문 (제목, 게재처) -> source_id는 논문 id
 */
public enum SearchSourceType {
    PROFILE,
    PUBLICATION
}
