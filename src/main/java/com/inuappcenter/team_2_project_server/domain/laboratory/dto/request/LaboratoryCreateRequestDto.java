package com.inuappcenter.team_2_project_server.domain.laboratory.dto.request;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// professorId는 요청으로 받지 않는다. 로그인한 교수 본인 명의로만 연구실을 생성할 수 있어,
// 담당 교수는 인증된 계정(Professor.member)에서 서버가 직접 결정한다
public record LaboratoryCreateRequestDto(
        @NotNull College college,
        @NotNull Department department,
        @NotBlank String labName,
        @NotBlank String location,
        @Valid @NotNull LaboratoryCapacityCreateDto capacity,
        String introduction,
        @NotBlank String labUrl,
        List<String> researchAreas
) {
}
