package com.inuappcenter.team_2_project_server.domain.member.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table
public class Researcher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "validate_yn")
    boolean validateYN;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true)
    Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    Laboratory laboratory;

    private Researcher(
            Member member,
            Laboratory laboratory
    ) {
        this.member = member;
        this.laboratory = laboratory;
    }

    public static Researcher create(
            Member member,
            Laboratory laboratory
    ) {
        if (laboratory == null) {
            throw new MyException(ErrorCode.LABORATORY_NOT_FOUND);
        }
        return new Researcher(member, laboratory);
    }
}
