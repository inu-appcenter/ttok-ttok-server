package com.inuappcenter.team_2_project_server.domain.bookmark.service;

import com.inuappcenter.team_2_project_server.domain.bookmark.dto.BookmarkResponseDto;
import com.inuappcenter.team_2_project_server.domain.bookmark.entity.Bookmark;
import com.inuappcenter.team_2_project_server.domain.bookmark.repository.BookmarkRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final LaboratoryService laboratoryService;

    /**
     * 내 북마크 목록 조회 컨트롤러
     */
    @Transactional(readOnly = true)
    public List<BookmarkResponseDto> getMyBookmark(
            Long memberId
    ) {
        return bookmarkRepository.findAllByMemberId(memberId)
                .stream()
                .map(BookmarkResponseDto::from)
                .toList();
    }

    /**
     * 북마크 토글 컨트롤러
     */
    @Transactional
    public void toggleBookmark(
            Long memberId,
            Long laboratoryId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.MEMBER_NOT_FOUND));
        Laboratory laboratory = laboratoryService.getLaboratoryEntity(laboratoryId);

        Optional<Bookmark> optionalBookMark = bookmarkRepository.findByMemberIdAndLaboratoryId(memberId, laboratoryId);

        if (optionalBookMark.isPresent()) {
            bookmarkRepository.delete(optionalBookMark.get());
            log.info("연구실 북마크 취소 - member: {}, laboratory: {}", memberId, laboratoryId);
        } else {
            // 더블클릭 등으로 동시에 두 번 등록 시도가 들어와 유니크 제약에 걸려도,
            // 결과적으로 "북마크된 상태"라는 목표는 이미 달성됐으므로 에러로 보지 않고 조용히 넘어간다
            try {
                bookmarkRepository.save(Bookmark.create(member, laboratory));
                log.info("연구실 북마크 등록 - member: {}, laboratory: {}", memberId, laboratoryId);
            } catch (DataIntegrityViolationException e) {
                log.info("연구실 북마크 등록 중복 시도(이미 등록됨) - member: {}, laboratory: {}", memberId, laboratoryId);
            }
        }
    }
}
