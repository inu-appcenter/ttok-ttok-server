package com.inuappcenter.team_2_project_server.domain.coffeeChat.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.enums.ContactType;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
public class CoffeeChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coffee_chat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "researcher_id", unique = true)
    private Researcher researcher;

    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    private String contactValue;

    private CoffeeChat(
            Laboratory laboratory,
            Researcher researcher,
            ContactType contactType,
            String contactValue
    ) {
        this.laboratory = laboratory;
        this.researcher = researcher;
        this.contactType = contactType;
        this.contactValue = contactValue;
    }

    public static CoffeeChat create(
            Laboratory laboratory,
            Researcher researcher,
            ContactType contactType,
            String contactValue
    ) {
        return new CoffeeChat(laboratory, researcher, contactType, contactValue);
    }

    public void update(
            ContactType contactType,
            String contactValue
    ) {
        this.contactType = contactType;
        this.contactValue = contactValue;
    }

    public boolean isOwnedBy(Long memberId) {
        return researcher.getMember().getId().equals(memberId);
    }
}
