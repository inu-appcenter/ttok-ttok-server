package com.inuappcenter.team_2_project_server.domain.member.dto.request;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;

public record MemberUpdateRequestDto(
        String nickName,
        Department department,
        String email
) {
}
