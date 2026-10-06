package com.inuappcenter.team_2_project_server.domain.labProject.dto;

import jakarta.validation.constraints.NotNull;

public record ResearchProjectAssignRequestDto(
        @NotNull
        Long laboratoryId
) {
}
