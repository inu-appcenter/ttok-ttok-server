package com.inuappcenter.team_2_project_server.domain.laboratory.repository;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow;
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

    // 이 연구실의 담당 교수와 연동된 계정인지 확인 (교수 본인 수정 권한 검증용)
    boolean existsByIdAndProfessor_MemberId(Long laboratoryId, Long memberId);

    // 이 교수가 이미 명의로 된 연구실이 있는지 확인 (연구실 생성은 아직 없는 교수만 가능)
    boolean existsByProfessorId(Long professorId);

    // 검색어(연구실명/교수명) + 단과대 + 학과 + 연구분야 조합 검색
    // 값이 없는 조건은 '전체'로 처리한다. 문자열 조건(keyword, researchArea)은 null 대신 빈 문자열로 받아 전체를 의미한다.
    // 연구분야는 join 대신 exists로 걸러서 distinct 없이 페이징이 정확하게 되도록 한다.
    // keyword는 서비스에서 %, _ 를 이스케이프해서 넘기므로 escape 문자를 지정한다.
    @EntityGraph(attributePaths = "professor")
    @Query("""
            select l from Laboratory l
            left join l.professor p
            where (:keyword = ''
                    or lower(l.labName) like lower(concat('%', :keyword, '%')) escape '\\'
                    or lower(p.name) like lower(concat('%', :keyword, '%')) escape '\\')
            and (:college is null or l.college = :college)
            and (:department is null or l.department = :department)
            and (:researchArea = '' or exists (
                    select 1 from LaboratoryResearchArea lra
                    where lra.laboratory = l and lra.researchKeyword.area = :researchArea
            ))
            """)
    Page<Laboratory> searchByFilter(
            @Param("keyword") String keyword,
            @Param("college") College college,
            @Param("department") Department department,
            @Param("researchArea") String researchArea,
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

    @Query("""
            select new com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow(
                l.college, l.department, count(l)
            )
            from Laboratory l
            group by l.college, l.department
            """)
    List<LabCollegeDeptCountRow> countGroupByCollegeAndDepartment();
}
