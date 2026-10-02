package com.inuappcenter.team_2_project_server.domain.researchMetric.dto.response;

import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.researchMetric.entity.ResearchMetric;

import java.time.LocalDateTime;

/**
 * 연구실 연구 지표 응답
 * - hIndex, citationCount: OpenAlex 저자 매칭 전이거나 아직 동기화 전이면 null
 * - recentPublicationCount: 우리 DB 논문 데이터로 집계 (최근 5년)
 */
public record ResearchMetricResponseDto(
        Long professorId,
        String professorName,
        Integer hIndex,
        Integer citationCount,
        long recentPublicationCount,
        LocalDateTime syncedAt
) {
    public static ResearchMetricResponseDto of(Professor professor, ResearchMetric metric, long recentPublicationCount) {
        return new ResearchMetricResponseDto(
                professor.getId(),
                professor.getName(),
                metric == null ? null : metric.getHIndex(),
                metric == null ? null : metric.getCitationCount(),
                recentPublicationCount,
                metric == null ? null : metric.getSyncedAt()
        );
    }
}
