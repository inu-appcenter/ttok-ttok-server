package com.inuappcenter.team_2_project_server.domain.member.repository;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByDepartmentAndNameAndEmail(Department department, String name, String email);
}
