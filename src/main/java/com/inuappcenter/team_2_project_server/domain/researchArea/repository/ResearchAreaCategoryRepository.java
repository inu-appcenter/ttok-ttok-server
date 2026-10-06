package com.inuappcenter.team_2_project_server.domain.researchArea.repository;

import com.inuappcenter.team_2_project_server.domain.researchArea.entity.ResearchAreaCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResearchAreaCategoryRepository extends JpaRepository<ResearchAreaCategory, Long> {
    Optional<ResearchAreaCategory> findByCategoryName(String categoryName);
}
