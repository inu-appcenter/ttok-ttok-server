package com.inuappcenter.team_2_project_server.domain.member.repository;

import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResearcherRepository extends JpaRepository<Researcher, Long> {
    Optional<Researcher> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    boolean existsByMemberIdAndLaboratoryId(Long memberId, Long laboratoryId);
}
