package com.inuappcenter.team_2_project_server.domain.department.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;

public record CollegeResponseDto(
        College college,
        String collegeName
) {
    public static CollegeResponseDto from(College college) {
        return new CollegeResponseDto(
                college,
                college.getCollegeName()
        );
    }
}
