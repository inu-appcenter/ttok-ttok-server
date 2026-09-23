package com.inuappcenter.team_2_project_server.domain.ai.service;

import com.inuappcenter.team_2_project_server.domain.ai.dto.AiRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.AiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class AiService {

    private final WebClient aiWebClient;
    private final String researchLabChatBotId;
    private final String emailEditorChatBotId;

    public AiService(
            WebClient aiWebClient,
            @Value("${ai.research-lab-chatbot-id}") String researchLabChatBotId,
            @Value("${ai.email-editor-chatbot-id}") String emailEditorChatBotId
    ) {
        this.aiWebClient = aiWebClient;
        this.researchLabChatBotId = researchLabChatBotId;
        this.emailEditorChatBotId = emailEditorChatBotId;
    }

    /**
     * 연구실 추천 챗봇 호출 메서드
     */
    public AiResponseDto researchLabAsk(String question) {
        return ask(question, researchLabChatBotId);
    }

    /**
     * 이메일 교정 챗봇 호출 메서드
     */
    public AiResponseDto emailEditorAsk(String question) {
        return ask(question, emailEditorChatBotId);
    }

    /**
     * 공통 로직 추출
     * 1. research_lab_chatBot
     * 2. email_editor_chatBot
     */
    private AiResponseDto ask(String question, String chatBotId) {
        AiRequestDto request = new AiRequestDto(List.of(new AiRequestDto.Message("user", question)));
        return aiWebClient.post()
                .uri("/v1/gateway/chatbots/{chatbotId}/chat/completions/", chatBotId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponseDto.class)
                .block();
    }
}
