package com.inuappcenter.team_2_project_server.domain.coffeeChat.repository;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.entity.CoffeeChat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoffeeChatRepository extends JpaRepository<CoffeeChat, Long> {

    @EntityGraph(attributePaths = "laboratory")
    List<CoffeeChat> findAllByLaboratoryId(Long laboratoryId);

    Optional<CoffeeChat> findByResearcherMemberId(Long memberId);

    boolean existsByResearcherMemberId(Long memberId);
}
