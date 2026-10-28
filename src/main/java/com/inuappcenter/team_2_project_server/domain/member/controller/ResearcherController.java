package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ResearcherRegisterRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/researcher")
@Slf4j
public class ResearcherController implements ResearcherApiSpecification {

    private final ResearcherService researcherService;

    /**
     * 연구자 등록 컨트롤러
     */
    @PostMapping
    public ResponseEntity<ResponseDto<ResearcherResponseDto>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ResearcherRegisterRequestDto request
    ) {
        ResearcherResponseDto response = researcherService.register(
                member.getId(),
                request.laboratoryId()
        );

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구자 등록 성공")
        );
    }

    /**
     * 연구자 조회 컨트롤러 (memberId 기준)
     */
    @GetMapping
    public ResponseEntity<ResponseDto<ResearcherResponseDto>> getResearcher(
            @RequestParam Long memberId
    ) {
        ResearcherResponseDto response = researcherService.getByMemberId(memberId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구자 조회 성공")
        );
    }

    /**
     * 내 연구자 등록 해제 컨트롤러
     */
    @DeleteMapping("/me")
    public ResponseEntity<ResponseDto<Long>> deleteResearcher(
            @AuthenticationPrincipal Member member
    ) {
        researcherService.deleteByMemberId(member.getId());
        log.info(member.getId() + " 연구자가 삭제되었습니다.");
        return ResponseEntity.ok(ResponseDto.of(member.getId(), member.getId() + "번 유저 연구자 삭제 성공"));
    }
}
