package com.inuappcenter.team_2_project_server.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 교수-계정 연동 요청 DTO
 */
public record ProfessorLinkRequestDto(
        @NotNull Long professorId,
        @NotNull Long memberId
) {
}
