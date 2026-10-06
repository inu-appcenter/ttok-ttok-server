package com.inuappcenter.team_2_project_server.domain.researchArea.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연구실과 쪼갠 키워드를 연결하는 중간 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "laboratory_research_keyword",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_research_keyword",
                        columnNames = {"laboratory_id", "research_keyword_id"}
                )
        }
)
public class LaboratoryResearchArea extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "laboratory_research_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_keyword_id", nullable = false)
    private ResearchArea researchKeyword;

    private LaboratoryResearchArea(
            Laboratory laboratory,
            ResearchArea researchKeyword
    ) {
        this.laboratory = laboratory;
        this.researchKeyword = researchKeyword;
    }

    public static LaboratoryResearchArea create(
            Laboratory laboratory,
            ResearchArea researchKeyword
    ) {
        return new LaboratoryResearchArea(laboratory, researchKeyword);
    }
}
