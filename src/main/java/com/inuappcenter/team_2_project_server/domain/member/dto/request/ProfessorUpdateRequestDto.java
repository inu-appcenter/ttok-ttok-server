package com.inuappcenter.team_2_project_server.domain.member.dto.request;

/**
 * 교수 본인 정보 수정 요청 DTO. 요청에 포함된 값만 수정한다.
 * 학과/이름은 uk_professor_department_name_email 유니크 제약 및 엑셀 재매칭 키와 얽혀있어 여기서 다루지 않는다.
 */
public record ProfessorUpdateRequestDto(
        String positionRaw,
        String phoneNumber,
        String email
) {
}
