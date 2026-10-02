package com.inuappcenter.team_2_project_server.domain.member.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;

public record ProfessorResponseDto(
        Long id,
        String positionRaw,
        College college,
        String collegeName,
        Department department,
        String departmentName,
        String name,
        String phoneNumber,
        String email
) {

    public static ProfessorResponseDto from(Professor professor) {
        return new ProfessorResponseDto(
                professor.getId(),
                professor.getPositionRaw(),
                professor.getCollege(),
                professor.getCollege().getCollegeName(),
                professor.getDepartment(),
                professor.getDepartment().getDepartmentName(),
                professor.getName(),
                professor.getPhoneNumber(),
                professor.getEmail()
        );
    }
}
