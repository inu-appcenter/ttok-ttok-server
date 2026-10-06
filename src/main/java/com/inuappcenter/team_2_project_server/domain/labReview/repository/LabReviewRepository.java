package com.inuappcenter.team_2_project_server.domain.labReview.repository;

import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabReviewRepository extends JpaRepository<LabReview, Long> {
    boolean existsByResearcherId(Long researcherId);

    @EntityGraph(attributePaths = "doings")
    List<LabReview> findAllByLaboratoryId(Long laboratoryId);

    Optional<LabReview> findByResearcherId(Long researcherId);
}
