package com.inuappcenter.team_2_project_server.domain.researchArea.repository;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.researchArea.entity.LaboratoryResearchArea;
import com.inuappcenter.team_2_project_server.domain.researchArea.entity.ResearchArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LaboratoryResearchKeywordRepository extends JpaRepository<LaboratoryResearchArea, Long> {
    boolean existsByLaboratoryAndResearchKeyword(Laboratory laboratory, ResearchArea researchKeyword);

    List<LaboratoryResearchArea> findAllByLaboratory(Laboratory laboratory);
}
