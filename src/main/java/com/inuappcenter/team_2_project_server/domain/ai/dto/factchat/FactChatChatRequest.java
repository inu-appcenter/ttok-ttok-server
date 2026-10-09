package com.inuappcenter.team_2_project_server.domain.ai.dto.factchat;

import java.util.List;

/**
 * FactChat 게이트웨이 채팅 요청 (OpenAI chat completions 형식)
 * 기존 챗봇 API(/chatbots/{id})와 달리 모델과 시스템 프롬프트를 우리가 직접 정한다
 */
public record FactChatChatRequest(
        String model,
        List<Message> messages,
        double temperature
) {
    public record Message(
            String role,
            String content
    ) {
    }
}
