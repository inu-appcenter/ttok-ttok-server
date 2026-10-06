package com.inuappcenter.team_2_project_server.domain.researchArea.repository;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.researchArea.entity.LaboratoryResearchArea;
import com.inuappcenter.team_2_project_server.domain.researchArea.entity.ResearchArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryResearchKeywordRepository extends JpaRepository<LaboratoryResearchArea, Long> {
    boolean existsByLaboratoryAndResearchKeyword(Laboratory laboratory, ResearchArea researchKeyword);
}
