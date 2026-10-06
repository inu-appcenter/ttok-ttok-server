package com.inuappcenter.team_2_project_server.domain.publication.controller;

import com.inuappcenter.team_2_project_server.domain.publication.dto.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.domain.publication.service.PublicationService;
import com.inuappcenter.team_2_project_server.global.dto.PageResponseDto;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// URL은 연구실 하위 리소스(/api/laboratory/{laboratoryId}/publications)라 기존 경로를 그대로 유지
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/laboratory")
public class PublicationController implements PublicationApiSpecification {
    private final PublicationService publicationService;

    @Override
    @GetMapping("/{laboratoryId}/publications")
    public ResponseEntity<ResponseDto<PageResponseDto<PublicationResponseDto>>> getPublications(
            @PathVariable Long laboratoryId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(validatePage(page), 5, Sort.by(Sort.Direction.DESC, "year"));
        Page<PublicationResponseDto> result = publicationService.getLabPublications(laboratoryId, pageable);
        return ResponseEntity.ok(
                ResponseDto.of(PageResponseDto.from(result), "연구실 논문 목록 조회 성공")
        );
    }

    // 음수 페이지가 들어오면 PageRequest에서 500이 나므로 400으로 막음
    private int validatePage(int page) {
        if (page < 0) {
            throw new MyException(ErrorCode.INVALID_INPUT);
        }
        return page;
    }
}
