package com.inuappcenter.team_2_project_server.domain.laboratory.dto.response;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Publication;

public record PublicationResponseDto(
        Long id,
        String title,
        String researchersRaw,
        String platform,
        String year,
        String type,
        String status,
        String doi,
        String sourceURL
) {
    public static PublicationResponseDto from(Publication publication) {
        return new PublicationResponseDto(
                publication.getId(),
                publication.getTitle(),
                publication.getResearchersRaw(),
                publication.getPlatform(),
                publication.getYear(),
                publication.getType(),
                publication.getStatus(),
                publication.getDoi(),
                publication.getSourceURL()
        );
    }
}
