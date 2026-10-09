package com.inuappcenter.team_2_project_server.domain.ai.dto.factchat;

import java.util.List;

/**
 * FactChat 게이트웨이 임베딩 요청 (OpenAI 임베딩 API와 같은 형식)
 * input에 문장 여러 개를 넣으면 한 번의 호출로 각각의 벡터를 받는다
 */
public record FactChatEmbeddingRequest(
        String model,
        List<String> input
) {
}
