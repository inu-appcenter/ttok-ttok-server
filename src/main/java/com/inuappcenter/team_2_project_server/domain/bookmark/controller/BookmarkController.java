package com.inuappcenter.team_2_project_server.domain.bookmark.controller;

import com.inuappcenter.team_2_project_server.domain.bookmark.dto.BookmarkResponseDto;
import com.inuappcenter.team_2_project_server.domain.bookmark.service.BookmarkService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmark")
public class BookmarkController implements BookmarkApiSpecification {

    private final BookmarkService bookmarkService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ResponseDto<List<BookmarkResponseDto>>> getMyBookmark(
            @AuthenticationPrincipal Member member
    ) {
        List<BookmarkResponseDto> responses = bookmarkService.getMyBookmark(member.getId());

        return ResponseEntity.ok(
                ResponseDto.of(responses, "내 북마크 목록 조회 성공")
        );
    }

    @Override
    @PostMapping
    public ResponseEntity<ResponseDto<Long>> toggleBookmark(
            @AuthenticationPrincipal Member member,
            @RequestParam Long laboratoryId
    ) {
        bookmarkService.toggleBookmark(member.getId(), laboratoryId);

        return ResponseEntity.ok(
                ResponseDto.of(laboratoryId, "선택한 연구실 북마크 토글 성공")
        );
    }
}
