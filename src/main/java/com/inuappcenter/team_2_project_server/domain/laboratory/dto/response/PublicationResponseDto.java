package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;

public record PublicationResponseDto(
        Long id,
        Laboratory laboratory,
        Professor professor,
        String title,
        String researchersRaw,
        String platform,
        String year,
        String type,
        String status,
        String doi,
        String sourceURL
) {
}
