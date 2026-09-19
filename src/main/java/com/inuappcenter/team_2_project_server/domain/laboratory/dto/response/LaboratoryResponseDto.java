package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LaboratoryCapacityDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;

import java.util.Arrays;
import java.util.List;

public record LaboratoryResponseDto(
        Long id,
        College college,
        String collegeName,
        Department department,
        String departmentName,
        String labName,
        String location,
        LaboratoryCapacityDto capacity,
        String introduction,
        ProfessorResponseDto professor,
        String labUrl,
        List<String> researchAreas
) {

    public static LaboratoryResponseDto from(Laboratory laboratory) {
        return new LaboratoryResponseDto(
                laboratory.getId(),
                laboratory.getCollege(),
                laboratory.getCollege().getCollegeName(),
                laboratory.getDepartment(),
                laboratory.getDepartment().getDepartmentName(),
                laboratory.getLabName(),
                laboratory.getLocation(),
                new LaboratoryCapacityDto(
                        laboratory.getGraduateStudentCount(),
                        laboratory.getUndergraduateStudentCount()
                ),
                laboratory.getIntroduction(),
                ProfessorResponseDto.from(laboratory.getProfessor()),
                laboratory.getLabUrl(),
                parseResearchAreas(laboratory.getResearchFieldRaw())
        );
    }

    private static List<String> parseResearchAreas(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(area -> !area.isBlank())
                .distinct()
                .toList();
    }
}
