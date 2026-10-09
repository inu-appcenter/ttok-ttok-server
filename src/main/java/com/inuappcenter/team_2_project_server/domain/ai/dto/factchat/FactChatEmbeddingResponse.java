package com.inuappcenter.team_2_project_server.domain.ai.dto.factchat;

import java.util.List;

/**
 * FactChat 게이트웨이 임베딩 응답
 * index는 요청 input의 몇 번째 문장에 대한 벡터인지 나타낸다 (응답 순서에 기대지 않고 index로 맞춘다)
 */
public record FactChatEmbeddingResponse(
        List<Data> data
) {
    public record Data(
            int index,
            float[] embedding
    ) {
    }
}
