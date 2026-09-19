package com.inuappcenter.team_2_project_server.domain.laboratory.repository;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByLaboratoryAndTitleAndYear(Laboratory laboratory, String title, String year);

    Page<Publication> findByLaboratory(Laboratory laboratory, Pageable pageable);
}
