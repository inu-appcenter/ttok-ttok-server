package com.inuappcenter.team_2_project_server.domain.recommendation.dto.row;

/**
 * 검색 1단계 결과: 후보 연구실과 질문과의 유사도 점수 (높을수록 가까움)
 */
public interface LabCandidateRow {
    Long getLaboratoryId();

    Double getScore();
}
