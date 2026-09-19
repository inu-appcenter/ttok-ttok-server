package com.inuappcenter.team_2_project_server.domain.laboratory.repository;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {
    // 단건(Optional)이 아닌 List로 받아 서비스 단에서 첫 번째 결과만 사용한다.
    List<Laboratory> findByLabNameAndDepartmentAndProfessor_Name(String labName, Department department, String professorName);

    boolean existsByLabNameAndProfessorAndDepartment(String labName, Professor professor, Department department);

    boolean existsByLabNameAndProfessorIdAndDepartment(String labName, Long professorId, Department department);

    @EntityGraph(attributePaths = "professor")
    Page<Laboratory> findByLabNameContainingIgnoreCaseOrProfessor_NameContainingIgnoreCase(
            String labNameKeyword,
            String professorNameKeyword,
            Pageable pageable
    );

    // 카테고리(상위 개념) -> 하위 연구분야 키워드 -> 연구실로 이어지는 조인 검색
    @EntityGraph(attributePaths = "professor")
    @Query("""
            select distinct l from Laboratory l
            join LaboratoryResearchArea lra on lra.laboratory = l
            join lra.researchKeyword ra
            where ra.category.categoryName = :categoryName
            """)
    Page<Laboratory> findByResearchAreaCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "professor")
    Page<Laboratory> findAll(Pageable pageable);
}
