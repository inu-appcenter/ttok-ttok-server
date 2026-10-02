package com.inuappcenter.team_2_project_server.domain.researchMetric.entity;

import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 교수(저자) 단위 연구 지표 (h-index, 피인용 수)
 * - OpenAlex에서 배치로 받아와 저장하고, 조회 시에는 이 테이블만 읽는다
 * - 5년 논문 수는 Publication 테이블에서 집계하므로 여기에 저장하지 않는다
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "research_metric")
public class ResearchMetric extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "research_metric_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false, unique = true)
    private Professor professor;

    // OpenAlex 저자 ID (예: A5067381195). 매칭 전에는 null
    @Column(name = "open_alex_author_id")
    private String openAlexAuthorId;

    // 관리자가 직접 지정한 저자 ID면 true. 배치의 자동 매칭이 덮어쓰지 않도록 구분
    @Column(name = "manually_matched", nullable = false)
    private boolean manuallyMatched;

    @Column(name = "h_index")
    private Integer hIndex;

    @Column(name = "citation_count")
    private Integer citationCount;

    // 마지막으로 OpenAlex에서 지표를 받아온 시각 (저자 ID 수정 등으로 바뀌는 updatedAt과 구분)
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    private ResearchMetric(Professor professor) {
        this.professor = professor;
    }

    public static ResearchMetric create(Professor professor) {
        return new ResearchMetric(professor);
    }

    // 배치의 자동 매칭 결과 반영. 관리자가 직접 지정한 경우에는 덮어쓰지 않는다
    public void matchAuthor(String openAlexAuthorId) {
        if (manuallyMatched) {
            return;
        }
        this.openAlexAuthorId = openAlexAuthorId;
    }

    // 관리자가 저자 ID를 직접 지정
    public void assignAuthorManually(String openAlexAuthorId) {
        this.openAlexAuthorId = openAlexAuthorId;
        this.manuallyMatched = true;
    }

    public void updateMetrics(Integer hIndex, Integer citationCount, LocalDateTime syncedAt) {
        this.hIndex = hIndex;
        this.citationCount = citationCount;
        this.syncedAt = syncedAt;
    }

    public boolean isMatched() {
        return openAlexAuthorId != null;
    }
}
