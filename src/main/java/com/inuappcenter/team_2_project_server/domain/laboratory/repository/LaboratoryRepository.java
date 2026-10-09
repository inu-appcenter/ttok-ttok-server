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

import java.util.Collection;
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

    // 카테고리(다중) + 단과대 + 학과 필터로 범위를 좁히고, 그 안에서 키워드로 검색한다. (조건끼리는 AND)
    // - 카테고리는 여러 개 중 하나라도 해당하면 포함(OR). categories가 비어 있으면(allCategories = true) 전체로 처리한다.
    // - 키워드는 연구실명/교수명/학과명/세부 연구분야명/카테고리명 중 하나라도 부분일치하면 포함(OR).
    //   공백을 무시하고 비교하도록 컬럼 값에서도 공백을 제거한다. keyword는 서비스에서 소문자 변환, 공백 제거, %, _ 이스케이프 후 넘긴다.
    //   학과는 enum이라 DB에서 비교할 수 없어, 서비스에서 한글 학과명이 키워드를 포함하는 학과 목록(keywordDepartments)을 만들어 넘긴다.
    // 연구분야/카테고리는 join 대신 exists로 걸러서 distinct 없이 페이징이 정확하게 되도록 한다.
    @EntityGraph(attributePaths = "professor")
    @Query("""
            select l from Laboratory l
            left join l.professor p
            where (:allCategories = true or exists (
                    select 1 from LaboratoryResearchArea lra
                    join lra.researchKeyword ra
                    join ra.category c
                    where lra.laboratory = l and c.categoryName in :categories
            ))
            and (:college is null or l.college = :college)
            and (:department is null or l.department = :department)
            and (:keyword = ''
                    or replace(lower(l.labName), ' ', '') like concat('%', :keyword, '%') escape '\\'
                    or replace(lower(p.name), ' ', '') like concat('%', :keyword, '%') escape '\\'
                    or l.department in :keywordDepartments
                    or exists (
                        select 1 from LaboratoryResearchArea klra
                        join klra.researchKeyword kra
                        left join kra.category kc
                        where klra.laboratory = l
                        and (replace(lower(kra.area), ' ', '') like concat('%', :keyword, '%') escape '\\'
                            or replace(lower(kc.categoryName), ' ', '') like concat('%', :keyword, '%') escape '\\')
                    ))
            """)
    Page<Laboratory> searchByFilter(
            @Param("allCategories") boolean allCategories,
            @Param("categories") List<String> categories,
            @Param("college") College college,
            @Param("department") Department department,
            @Param("keyword") String keyword,
            @Param("keywordDepartments") List<Department> keywordDepartments,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = "professor")
    Page<Laboratory> findAll(Pageable pageable);

    // 연구과제 동기화용: 트랜잭션 밖에서 담당교수 이름을 읽으므로 교수까지 함께 조회 (지연 로딩 시 세션이 없어 실패함)
    @EntityGraph(attributePaths = "professor")
    List<Laboratory> findAllBy();

    // 연구실 추천 응답용: 추천된 연구실들을 교수까지 한 번에 조회 (트랜잭션 밖에서 교수 이름을 읽으므로)
    @EntityGraph(attributePaths = "professor")
    List<Laboratory> findWithProfessorByIdIn(Collection<Long> ids);

    // 연구 지표 동기화 대상: 연구실을 가진 교수 (연구실 없는 교수까지 조회하면 외부 API 호출만 낭비됨)
    @Query("select distinct l.professor from Laboratory l where l.professor is not null")
    List<Professor> findAllProfessorsHavingLaboratory();

    @Query("""
            select new com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow(
                l.college, l.department, count(l)
            )
            from Laboratory l
            group by l.college, l.department
            """)
    List<LabCollegeDeptCountRow> countGroupByCollegeAndDepartment();
}
