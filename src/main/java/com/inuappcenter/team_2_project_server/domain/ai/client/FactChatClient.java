package com.inuappcenter.team_2_project_server.domain.ai.client;

import com.inuappcenter.team_2_project_server.domain.ai.dto.factchat.FactChatChatRequest;
import com.inuappcenter.team_2_project_server.domain.ai.dto.factchat.FactChatChatResponse;
import com.inuappcenter.team_2_project_server.domain.ai.dto.factchat.FactChatEmbeddingRequest;
import com.inuappcenter.team_2_project_server.domain.ai.dto.factchat.FactChatEmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * MindLogic FactChat 게이트웨이 API 연동 (/embeddings, /chat/completions). 모델과 프롬프트를 우리가 직접 지정한다.
 * (연구실 추천 RAG, 교수님께 메일 쓰기)
 * <p>
 * 호출 실패는 NtisClient, OpenAlexClient와 마찬가지로 예외 대신 로그 + Optional.empty()로 돌려주고,
 * 실패했을 때 어떻게 할지(오류 응답, 대체 응답, 다음 배치에서 재시도)는 호출하는 서비스가 정한다.
 */
@Slf4j
@Component
public class FactChatClient {

    private static final String EMBEDDINGS_PATH = "/v1/gateway/embeddings/";
    private static final String CHAT_COMPLETIONS_PATH = "/v1/gateway/chat/completions/";

    private final WebClient factChatGatewayWebClient;

    public FactChatClient(
            @Qualifier("factChatGatewayWebClient") WebClient factChatGatewayWebClient
    ) {
        this.factChatGatewayWebClient = factChatGatewayWebClient;
    }

    /**
     * 문장들을 임베딩 벡터로 바꾼다. 반환 목록의 순서는 texts의 순서와 같다.
     */
    public Optional<List<float[]>> embed(String model, List<String> texts) {
        try {
            FactChatEmbeddingResponse response = factChatGatewayWebClient.post()
                    .uri(EMBEDDINGS_PATH)
                    .bodyValue(new FactChatEmbeddingRequest(model, texts))
                    .retrieve()
                    .bodyToMono(FactChatEmbeddingResponse.class)
                    .block();

            if (response == null || response.data() == null || response.data().size() != texts.size()) {
                log.warn("FactChat 임베딩 응답 개수 불일치 (요청 {}개)", texts.size());
                return Optional.empty();
            }

            return Optional.of(response.data().stream()
                    .sorted(Comparator.comparingInt(FactChatEmbeddingResponse.Data::index))
                    .map(FactChatEmbeddingResponse.Data::embedding)
                    .toList());
        } catch (WebClientResponseException e) {
            log.warn("FactChat 임베딩 호출 실패 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("FactChat 임베딩 호출 실패", e);
            return Optional.empty();
        }
    }

    /**
     * LLM에 시스템 프롬프트와 사용자 메시지를 보내고 답변 텍스트를 받는다.
     * temperature 0: 같은 입력에는 최대한 같은 답이 나오도록 한다 (추천 결과, 메일 초안이 매번 바뀌지 않게)
     */
    public Optional<String> chat(String model, String systemPrompt, String userMessage) {
        FactChatChatRequest request = new FactChatChatRequest(
                model,
                List.of(
                        new FactChatChatRequest.Message("system", systemPrompt),
                        new FactChatChatRequest.Message("user", userMessage)
                ),
                0
        );

        try {
            FactChatChatResponse response = factChatGatewayWebClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FactChatChatResponse.class)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null) {
                log.warn("FactChat 채팅 응답이 비어 있음 (model={})", model);
                return Optional.empty();
            }

            return Optional.ofNullable(response.choices().getFirst().message().content());
        } catch (WebClientResponseException e) {
            log.warn("FactChat 채팅 호출 실패 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("FactChat 채팅 호출 실패", e);
            return Optional.empty();
        }
    }
}
