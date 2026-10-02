package com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;

public record PublicationExcelRow(
        String laboratoryNumber,
        Department department,
        String laboratoryName,
        String professorName,
        String title,
        String researchersRaw,
        String platform,
        String year,
        String type,
        String status,
        String doi,
        String sourceURL
) {
}
