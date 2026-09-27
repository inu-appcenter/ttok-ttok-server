package com.inuappcenter.team_2_project_server.domain.member.dto.response;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;

import java.time.LocalDateTime;

public record MemberResponseDto(
        Long id,
        String studentNumber,
        String nickName,
        Department department,
        String email,
        LocalDateTime lastLoginAt,
        boolean isNew,
        UserType userType
) {
    // 엔티티는 Dto로 바꾸는 정적 팩토리 메서드
    public static MemberResponseDto from(
            Member member
    ) {
        return new MemberResponseDto(
                member.getId(),
                member.getStudentNumber(),
                member.getNickName(),
                member.getDepartment(),
                member.getEmail(),
                member.getLastLoginAt(),
                member.isNew(),
                member.getUserType()
        );
    }
}
