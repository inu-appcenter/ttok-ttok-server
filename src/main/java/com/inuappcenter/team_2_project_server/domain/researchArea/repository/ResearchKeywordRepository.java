package com.inuappcenter.team_2_project_server.domain.researchArea.repository;

import com.inuappcenter.team_2_project_server.domain.researchArea.entity.ResearchArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResearchKeywordRepository extends JpaRepository<ResearchArea, Long> {
    Optional<ResearchArea> findByArea(String name);
}
