package com.inuappcenter.team_2_project_server.domain.bookmark.repository;

import com.inuappcenter.team_2_project_server.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    @EntityGraph(attributePaths = {"laboratory", "laboratory.professor"})
    List<Bookmark> findAllByMemberId(Long memberId);

    Optional<Bookmark> findByMemberIdAndLaboratoryId(Long memberId, Long laboratoryId);
}
