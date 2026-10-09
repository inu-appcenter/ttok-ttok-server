package com.inuappcenter.team_2_project_server.domain.ai.dto.row;

/**
 * 인덱싱 원본: 연구실에 연결된 논문의 검색에 쓰는 필드만
 */
public interface PublicationSourceRow {
    Long getId();

    Long getLaboratoryId();

    String getTitle();

    String getPlatform();
}
