package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LabCollegeDeptCountRow;

public record LabCountByDeptResponseDto(
        Department department, String departmentName, Long count
) {
    public static LabCountByDeptResponseDto from(LabCollegeDeptCountRow row) {
        return new LabCountByDeptResponseDto(
                row.department(),
                row.department().getDepartmentName(),
                row.count()
        );
    }
}
