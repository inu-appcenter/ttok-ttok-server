package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;

import java.util.List;

public record LabCountByCollegeResponseDto(
        College college, String collegeName,
        List<LabCountByDeptResponseDto> departments
) {
    public static LabCountByCollegeResponseDto of(
            College college,
            List<LabCountByDeptResponseDto> departments
    ) {
        return new LabCountByCollegeResponseDto(
                college,
                college.getCollegeName(),
                departments
        );
    }
}
