package com.inuappcenter.team_2_project_server.domain.bookmark.dto;

import com.inuappcenter.team_2_project_server.domain.bookmark.entity.Bookmark;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;

public record BookmarkResponseDto(
        Long id,
        LaboratoryResponseDto laboratory
) {
    public static BookmarkResponseDto from(
            Bookmark bookmark
    ) {
        return new BookmarkResponseDto(
                bookmark.getId(),
                LaboratoryResponseDto.from(bookmark.getLaboratory())
        );
    }
}
