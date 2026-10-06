package com.inuappcenter.team_2_project_server.domain.publication.repository;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.publication.entity.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByLaboratoryAndTitleAndYear(Laboratory laboratory, String title, String year);

    Page<Publication> findByLaboratory(Laboratory laboratory, Pageable pageable);

    // 연구 지표: 교수 ↔ OpenAlex 저자 매칭에 쓸 최근 논문 DOI 목록
    @Query("""
            select p.doi from Publication p
            where p.professor.id = :professorId
            and p.doi is not null and p.doi <> ''
            order by p.year desc nulls last
            """)
    List<String> findRecentDoisByProfessorId(@Param("professorId") Long professorId, Pageable pageable);

    // 연구 지표: 최근 N년 논문 수. year는 "2024" 형태의 문자열이라 문자열 비교로 처리
    long countByProfessorIdAndYearGreaterThanEqual(Long professorId, String year);
}
