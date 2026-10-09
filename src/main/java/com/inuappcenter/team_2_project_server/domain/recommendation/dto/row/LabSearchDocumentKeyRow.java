package com.inuappcenter.team_2_project_server.domain.recommendation.dto.row;

import com.inuappcenter.team_2_project_server.domain.recommendation.enums.SearchSourceType;

/**
 * 인덱싱 비교용 검색 문서 요약. 벡터(문서당 숫자 1536개)는 크기가 커서 읽지 않는다
 */
public interface LabSearchDocumentKeyRow {
    Long getId();

    SearchSourceType getSourceType();

    Long getSourceId();

    String getContentHash();

    String getEmbeddingModel();
}
