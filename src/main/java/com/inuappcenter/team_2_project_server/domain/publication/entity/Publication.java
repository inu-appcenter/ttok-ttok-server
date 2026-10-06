package com.inuappcenter.team_2_project_server.domain.publication.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
public class Publication extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publication_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    // 논문 제목은 부제/영문 제목 등으로 255자를 쉽게 넘겨서 길이 제한 없이 저장
    @Column(columnDefinition = "TEXT")
    private String title;

    // 공저자가 많으면 목록이 255자를 넘길 수 있어서 길이 제한 없이 저장
    @Column(name = "researchers_raw", columnDefinition = "TEXT")
    private String researchersRaw;

    private String platform;

    private String year;

    private String type;

    private String status;

    private String doi;

    @Column(name = "source_url")
    private String sourceURL;

    private Publication(
            Laboratory laboratory,
            Professor professor,
            String title,
            String researchersRaw,
            String platform,
            String year,
            String type,
            String status,
            String doi,
            String sourceURL
    ) {
        this.laboratory = laboratory;
        this.professor = professor;
        this.title = title;
        this.researchersRaw = researchersRaw;
        this.platform = platform;
        this.year = year;
        this.type = type;
        this.status = status;
        this.doi = doi;
        this.sourceURL = sourceURL;
    }

    public static Publication create(
            Laboratory laboratory,
            Professor professor,
            String title,
            String researchersRaw,
            String platform,
            String year,
            String type,
            String status,
            String doi,
            String sourceURL
    ) {
        return new Publication(laboratory, professor, title, researchersRaw, platform, year, type, status, doi, sourceURL);
    }

}
