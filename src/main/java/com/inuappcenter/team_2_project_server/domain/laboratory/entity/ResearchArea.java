package com.inuappcenter.team_2_project_server.domain.laboratory.entity;

import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 엑셀 원문을 쪼개서 저장하는 엔티티(즉, 단어 하나하나를 의미)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_name",
                        columnNames = "area"
                )
        }
)
public class ResearchArea extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "research_area_id")
    private Long id;

    private String area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_category_id")
    private ResearchAreaCategory category;

    private ResearchArea(
            String area
    ) {
        this.area = area;
    }

    public static ResearchArea create(
            String area
    ) {
        return new ResearchArea(area);
    }

    public void updateCategory(ResearchAreaCategory category) {
        this.category = category;
    }
}
