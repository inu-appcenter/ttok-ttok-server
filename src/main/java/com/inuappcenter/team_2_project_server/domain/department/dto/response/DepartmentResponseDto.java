package com.inuappcenter.team_2_project_server.domain.department.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;

public record DepartmentResponseDto(
        Department department,
        String departmentName,
        College college,
        String collegeName
) {
    public static DepartmentResponseDto from(Department department) {
        return new DepartmentResponseDto(
                department,
                department.getDepartmentName(),
                department.getCollegeName(),
                department.getCollegeName().getCollegeName()
        );
    }
}
