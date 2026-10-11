package com.inuappcenter.team_2_project_server.ai;

import com.inuappcenter.team_2_project_server.domain.ai.client.FactChatClient;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.EmailRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.EmailResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.enums.EmailPurpose;
import com.inuappcenter.team_2_project_server.domain.ai.enums.Grade;
import com.inuappcenter.team_2_project_server.domain.ai.service.chatbot.EmailEditorService;
import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class EmailEditorServiceTest {

    private LaboratoryRepository laboratoryRepository;
    private FactChatClient factChatClient;
    private EmailEditorService emailEditorService;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        factChatClient = mock(FactChatClient.class);
        emailEditorService = new EmailEditorService(laboratoryRepository, factChatClient, "chat-model");
    }

    @Test
    void wraps_llm_body_with_fixed_greeting_intro_and_signature() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of(laboratory("권태현")));
        given(factChatClient.chat(eq("chat-model"), eq(EmailEditorService.SYSTEM_PROMPT), anyString()))
                .willReturn(Optional.of("\n  AI가 쓴 본문입니다.  \n"));

        EmailResponseDto response = emailEditorService.writeDraft(request(EmailPurpose.UNDERGRADUATE_RESEARCH, Grade.THIRD, "경험"));

        assertThat(response.subject()).isEqualTo("[학부연구생 지원 문의] 컴퓨터공학부 홍길동");
        assertThat(response.body()).isEqualTo("""
                안녕하세요, 권태현 교수님.
                컴퓨터공학부 3학년 홍길동입니다. 교수님 연구실의 학부연구생으로 지원하고 싶어 연락드렸습니다.

                AI가 쓴 본문입니다.

                감사합니다.
                홍길동 드림""");
    }

    @Test
    void sends_purpose_lab_info_and_student_input_to_llm_but_not_name_or_contact() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of(laboratory("권태현")));
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.of("본문"));

        emailEditorService.writeDraft(request(EmailPurpose.GRADUATE_CONSULTING, Grade.FIFTHPLUS, "  "));

        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(factChatClient).chat(eq("chat-model"), eq(EmailEditorService.SYSTEM_PROMPT), userMessage.capture());
        assertThat(userMessage.getValue()).contains(
                "[메일 목적]\n대학원 진학 상담",
                "연구실: 무기나노촉매 연구실",
                "담당 교수: 권태현",
                "연구 분야: 나노촉매, 수전해",
                "연구실 소개: 수소 생산 촉매를 연구합니다.",
                "컴퓨터공학부 5학년",
                "[관심 있는 연구]\n수전해 촉매",
                "[관련해서 해본 일]\n(없음)"   // 비어 있으면 (없음)
        );
        // 이름, 연락처는 본문에 쓰지 않으므로 넘기지 않는다
        assertThat(userMessage.getValue()).doesNotContain("홍길동", "ttokttok@gmail.com", "010-1234-5678");
    }

    @Test
    void greets_without_name_when_lab_has_no_professor() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of(laboratory(null)));
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.of("본문"));

        EmailResponseDto response = emailEditorService.writeDraft(request(EmailPurpose.QUESTION, Grade.FIRST, null));

        assertThat(response.subject()).isEqualTo("[질문 문의] 컴퓨터공학부 홍길동");
        assertThat(response.body()).startsWith("안녕하세요, 교수님.\n컴퓨터공학부 1학년 홍길동입니다. 교수님의 연구와 관련해 여쭙고 싶은 점이 있어 연락드렸습니다.");
    }

    @Test
    void fails_with_not_found_when_laboratory_does_not_exist() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of());

        assertThatThrownBy(() -> emailEditorService.writeDraft(request(EmailPurpose.QUESTION, Grade.FIRST, null)))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(ErrorCode.LABORATORY_NOT_FOUND);
        verifyNoInteractions(factChatClient);
    }

    @Test
    void unavailable_when_llm_call_fails() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of(laboratory("권태현")));
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailEditorService.writeDraft(request(EmailPurpose.QUESTION, Grade.FIRST, null)))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_DRAFT_UNAVAILABLE);
    }

    @Test
    void unavailable_when_llm_answer_is_blank() {
        given(laboratoryRepository.findWithProfessorByIdIn(List.of(1L))).willReturn(List.of(laboratory("권태현")));
        given(factChatClient.chat(anyString(), anyString(), anyString())).willReturn(Optional.of("   "));

        assertThatThrownBy(() -> emailEditorService.writeDraft(request(EmailPurpose.QUESTION, Grade.FIRST, null)))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_DRAFT_UNAVAILABLE);
    }

    private EmailRequestDto request(EmailPurpose purpose, Grade grade, String content) {
        return new EmailRequestDto(
                purpose, 1L, " 홍길동 ", Department.COMPUTER_ENGINEERING, grade,
                "ttokttok@gmail.com", "010-1234-5678", "수전해 촉매", content
        );
    }

    private Laboratory laboratory(String professorName) {
        Professor professor = professorName == null ? null
                : Professor.create(professorName, "교수", Department.CHEMISTRY, null, null);
        return Laboratory.create(
                College.COLLEGE_OF_NATURAL_SCIENCES, Department.CHEMISTRY, "무기나노촉매 연구실", null, 0, 0,
                "수소 생산 촉매를 연구합니다.", professor, null, "나노촉매, 수전해"
        );
    }
}
