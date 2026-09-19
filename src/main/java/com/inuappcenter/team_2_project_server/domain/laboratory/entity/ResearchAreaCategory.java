package com.inuappcenter.team_2_project_server.domain.laboratory.entity;

import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "research_area_category", uniqueConstraints = @UniqueConstraint(columnNames = "category_name"))
public class ResearchAreaCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "research_area_category_id")
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    private ResearchAreaCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public static ResearchAreaCategory create(String categoryName) {
        return new ResearchAreaCategory(categoryName);
    }
}
