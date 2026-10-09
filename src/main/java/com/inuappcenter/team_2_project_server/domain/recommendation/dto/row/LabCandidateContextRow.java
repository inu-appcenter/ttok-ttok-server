package com.inuappcenter.team_2_project_server.domain.recommendation.dto.row;

/**
 * LLM 프롬프트에 넣을 후보 연구실 정보: 프로필 원문 1개 + 질문과 가까운 논문 원문 몇 개
 */
public interface LabCandidateContextRow {
    Long getLaboratoryId();

    String getSourceType();

    String getContent();
}
