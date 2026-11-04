package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.member.controller.ResearcherController;
import com.inuappcenter.team_2_project_server.domain.member.dto.request.ResearcherRegisterRequestDto;
import com.inuappcenter.team_2_project_server.domain.member.dto.response.ResearcherResponseDto;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 연구자 등록·조회는 요청 값이 아니라 인증된 본인(memberId)을 기준으로만 동작해야 한다 (#93)
 */
class ResearcherControllerTest {

    private ResearcherService researcherService;
    private ResearcherController researcherController;
    private Member member;

    @BeforeEach
    void setUp() {
        researcherService = mock(ResearcherService.class);
        researcherController = new ResearcherController(researcherService);
        member = mock(Member.class);
        given(member.getId()).willReturn(1L);
    }

    @Test
    void getResearcher_queries_authenticated_member_only() {
        ResearcherResponseDto dto = new ResearcherResponseDto(10L, 1L, "20240001", 465L, "소프트웨어공학 연구실", false);
        given(researcherService.getByMemberId(1L)).willReturn(dto);

        ResponseEntity<ResponseDto<ResearcherResponseDto>> response = researcherController.getResearcher(member);

        assertThat(response.getBody().data()).isEqualTo(dto);
        verify(researcherService).getByMemberId(1L);
        verifyNoMoreInteractions(researcherService);
    }

    @Test
    void getResearcher_fails_when_authenticated_member_is_not_researcher() {
        given(researcherService.getByMemberId(1L)).willThrow(new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        assertThatThrownBy(() -> researcherController.getResearcher(member))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESEARCHER_NOT_FOUND);
    }

    @Test
    void register_uses_authenticated_member_id() {
        ResearcherResponseDto dto = new ResearcherResponseDto(10L, 1L, "20240001", 465L, "소프트웨어공학 연구실", false);
        given(researcherService.register(1L, 465L)).willReturn(dto);

        ResponseEntity<ResponseDto<ResearcherResponseDto>> response =
                researcherController.register(member, new ResearcherRegisterRequestDto(465L));

        assertThat(response.getBody().data()).isEqualTo(dto);
        verify(researcherService).register(1L, 465L);
        verifyNoMoreInteractions(researcherService);
    }
}
