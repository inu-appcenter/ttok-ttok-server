package com.inuappcenter.team_2_project_server.domain.ai.service.chatbot;

import com.inuappcenter.team_2_project_server.domain.ai.client.FactChatClient;
import com.inuappcenter.team_2_project_server.domain.ai.dto.AiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 이메일 작성/교정 (챗봇 기반)
 * 우리 데이터를 찾아 붙일 필요가 없는 작업이라 RAG를 쓰지 않고,
 * FactChat 관리 화면에서 모델과 프롬프트를 설정해 둔 챗봇에 질문을 그대로 전달한다.
 */
@Service
public class EmailEditorService {

    private final FactChatClient factChatClient;
    private final String emailEditorChatBotId;

    public EmailEditorService(
            FactChatClient factChatClient,
            @Value("${ai.email-editor-chatbot-id}") String emailEditorChatBotId
    ) {
        this.factChatClient = factChatClient;
        this.emailEditorChatBotId = emailEditorChatBotId;
    }

    /**
     * 이메일 교정 챗봇 호출 메서드
     */
    public AiResponseDto ask(String question) {
        return factChatClient.askChatbot(emailEditorChatBotId, question);
    }
}
