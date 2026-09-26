package com.inuappcenter.team_2_project_server.domain.member.controller;

import com.inuappcenter.team_2_project_server.domain.member.dto.request.ProfessorLinkRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ProfessorResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/professor")
public class ProfessorController implements ProfessorApiSpecification {

    private final ProfessorService professorService;

    @PostMapping("/link")
    public ResponseEntity<ResponseDto<ProfessorResponseDto>> linkMember(
            @Valid @RequestBody ProfessorLinkRequestDto request
    ) {
        ProfessorResponseDto response = professorService.linkMember(request.professorId(), request.memberId());

        return ResponseEntity.ok(
                ResponseDto.of(response, "교수 계정 연동 성공")
        );
    }
}
