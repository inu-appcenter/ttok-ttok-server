package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/professor")
public class ProfessorController implements ProfessorApiSpecification {

    private final ProfessorService professorService;

    @Override
    @PatchMapping("/me")
    public ResponseEntity<ResponseDto<ProfessorResponseDto>> updateMyProfile(
            @AuthenticationPrincipal Member member,
            @RequestBody ProfessorUpdateRequestDto request
    ) {
        ProfessorResponseDto response = professorService.updateMyProfile(member.getId(), request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "교수 정보 수정 성공")
        );
    }
}
