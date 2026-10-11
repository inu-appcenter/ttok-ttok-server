package com.inuappcenter.team_2_project_server.domain.ai.dto.row;

/**
 * 키워드 추천: 연구실이 가진 연구분야 카테고리 (연구실-카테고리 쌍)
 */
public interface LabCategoryRow {
    Long getLaboratoryId();

    String getCategoryName();
}
