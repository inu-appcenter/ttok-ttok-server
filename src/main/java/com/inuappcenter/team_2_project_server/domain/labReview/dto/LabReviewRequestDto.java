package com.inuappcenter.team_2_project_server.domain.labReview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record LabReviewRequestDto(
        @NotBlank @Size(max = 30) String coreTime,
        @NotBlank @Size(max = 30) String weeklyMeeting,
        @NotEmpty @Size(max = 20) Set<@NotBlank String> doings
) {
}
