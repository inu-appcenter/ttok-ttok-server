package com.inuappcenter.team_2_project_server.domain.bookmark.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "laboratory_id"}))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long id;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JoinColumn(name = "laboratory_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Laboratory laboratory;

    private Bookmark(
            Member member,
            Laboratory laboratory
    ) {
        this.member = member;
        this.laboratory = laboratory;
    }

    public static Bookmark create(
            Member member,
            Laboratory laboratory
    ) {
        return new Bookmark(member, laboratory);
    }
}
