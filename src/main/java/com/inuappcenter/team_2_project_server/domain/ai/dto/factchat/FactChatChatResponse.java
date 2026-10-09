package com.inuappcenter.team_2_project_server.domain.ai.dto.factchat;

import java.util.List;

/**
 * FactChat 게이트웨이 채팅 응답 (필요한 필드만)
 */
public record FactChatChatResponse(
        List<Choice> choices
) {
    public record Choice(
            Message message
    ) {
    }

    public record Message(
            String content
    ) {
    }
}
