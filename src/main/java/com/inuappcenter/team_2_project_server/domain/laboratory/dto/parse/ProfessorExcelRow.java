package com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;

public record ProfessorExcelRow(
        College college,
        Department department,
        String name,
        String position,
        String researchAreaRaw,
        String number,
        String email
) {
}
