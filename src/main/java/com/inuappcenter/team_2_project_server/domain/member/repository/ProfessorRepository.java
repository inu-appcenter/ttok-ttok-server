package com.inuappcenter.team_2_project_server.domain.member.repository;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByDepartmentAndNameAndEmail(Department department, String name, String email);

    // 계정과 연동된 교수는 이메일이 엑셀 원본과 달라졌을 수 있어 이름+학과만으로 우선 찾는다
    Optional<Professor> findByDepartmentAndNameAndMemberIsNotNull(Department department, String name);

    // 본인(로그인한 Member) 기준으로 연동된 교수 레코드 조회
    Optional<Professor> findByMemberId(Long memberId);
}
