package com.inuappcenter.team_2_project_server.domain.recommendation.dto.response;

/**
 * 검색 문서 인덱싱 결과
 * - created/updated: 새로 임베딩해 저장한 문서 수 / 원문이 바뀌어 다시 임베딩한 문서 수
 * - deleted: 원본(연구실, 논문)이 없어져 지운 문서 수
 * - failed: 임베딩 API 실패로 이번에 반영하지 못한 문서 수 (다음 동기화 때 다시 시도)
 * - unchanged: 원문이 그대로라 건너뛴 문서 수
 */
public record LabSearchIndexSyncResponseDto(
        int created,
        int updated,
        int deleted,
        int failed,
        int unchanged
) {
}
