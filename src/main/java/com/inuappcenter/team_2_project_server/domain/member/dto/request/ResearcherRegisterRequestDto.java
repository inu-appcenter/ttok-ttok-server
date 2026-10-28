package com.inuappcenter.team_2_project_server.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 연구자 등록 요청 DTO (등록 대상 회원은 인증 토큰에서 가져온다)
 */
public record ResearcherRegisterRequestDto(
        @NotNull Long laboratoryId
) {
}
