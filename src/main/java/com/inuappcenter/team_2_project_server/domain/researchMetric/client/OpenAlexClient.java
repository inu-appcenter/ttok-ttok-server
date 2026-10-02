package com.inuappcenter.team_2_project_server.domain.researchMetric.client;

import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.OpenAlexAuthorResponse;
import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.OpenAlexWorkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAlex 연동. 교수 연구 지표(h-index, 피인용 수)를 가져오고,
 * 논문 DOI로 교수 ↔ OpenAlex 저자를 매칭하는 데 쓴다.
 * NtisClient와 마찬가지로 연동 실패가 배치 전체를 멈추면 안 되므로,
 * 예외를 잡아 로그만 남기고 Optional.empty()를 반환한다.
 */
@Slf4j
@Component
public class OpenAlexClient {

    // 인천용대학교 OpenAlex 기관 ID. 저자 매칭 시 소속 확인에 사
    public static final String INU_INSTITUTION_ID = "https://openalex.org/I146429904";

    private static final String OPEN_ALEX_ID_PREFIX = "https://openalex.org/";
    private static final String DOI_URL_PREFIX = "https://doi.org/";

    // OpenAlex filter의 OR 조건은 최대 50개까지 허용
    public static final int MAX_DOIS_PER_REQUEST = 50;

    private final WebClient openAlexWebClient;
    private final String apiKey;

    public OpenAlexClient(
            @Qualifier("openAlexWebClient") WebClient openAlexWebClient,
            @Value("${openalex.api-key:}") String apiKey
    ) {
        this.openAlexWebClient = openAlexWebClient;
        this.apiKey = apiKey;
    }

    // "https://openalex.org/A123" → "A123"
    public static String toShortId(String openAlexId) {
        if (openAlexId == null) {
            return null;
        }
        String trimmed = openAlexId.trim();
        return trimmed.startsWith(OPEN_ALEX_ID_PREFIX) ? trimmed.substring(OPEN_ALEX_ID_PREFIX.length()) : trimmed;
    }

    /**
     * 저자 ID로 h-index, 피인용 수를 조회한다.
     *
     * @param authorId "A5067381195" 또는 "https://openalex.org/A5067381195"
     */
    public Optional<OpenAlexAuthorResponse> getAuthor(String authorId) {
        String shortId = toShortId(authorId);
        try {
            OpenAlexAuthorResponse response = openAlexWebClient.get()
                    .uri(uriBuilder -> withApiKey(uriBuilder
                            .path("/authors/{authorId}")
                            .queryParam("select", "id,display_name,summary_stats,cited_by_count"))
                            .build(shortId))
                    .retrieve()
                    .bodyToMono(OpenAlexAuthorResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return Optional.ofNullable(response);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("OpenAlex 저자 없음 (authorId={})", shortId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("OpenAlex 저자 조회 실패 (authorId={})", shortId, e);
            return Optional.empty();
        }
    }

    /**
     * 여러 DOI의 논문을 한 번에 조회해 저자 목록을 가져온다. (교수 ↔ OpenAlex 저자 매칭용)
     * OpenAlex filter는 "|"로 OR 조건을 지원해서, 교수 한 명당 API 호출 1번으로 끝낸다 (일일 무료 사용량 절약)
     * @param dois "10.1038/nature14539" 또는 "https://doi.org/10.1038/nature14539" 형식 (최대 MAX_DOIS_PER_REQUEST개)
     */
    public List<OpenAlexWorkResponse.Work> findWorksByDois(List<String> dois) {
        List<String> normalizedDois = dois.stream()
                .map(this::normalizeDoi)
                .filter(doi -> !doi.isEmpty() && !doi.contains("|"))
                .distinct()
                .limit(MAX_DOIS_PER_REQUEST)
                .toList();
        if (normalizedDois.isEmpty()) {
            return List.of();
        }

        try {
            // DOI에 특수문자가 섞여 있어도 안전하게 인코딩되도록 값은 템플릿 변수로 넘긴다
            Map<String, Object> variables = new HashMap<>();
            variables.put("filter", "doi:" + String.join("|", normalizedDois));

            OpenAlexWorkResponse response = openAlexWebClient.get()
                    .uri(uriBuilder -> withApiKey(uriBuilder
                            .path("/works")
                            .queryParam("filter", "{filter}")
                            .queryParam("select", "id,doi,authorships")
                            .queryParam("per_page", normalizedDois.size()))
                            .build(variables))
                    .retrieve()
                    .bodyToMono(OpenAlexWorkResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return response == null ? List.of() : response.resultsOrEmpty();
        } catch (Exception e) {
            log.error("OpenAlex 논문 조회 실패 (dois={})", normalizedDois, e);
            return List.of();
        }
    }

    // API 키가 설정된 경우에만 붙인다 (키 없이도 일일 무료 사용량 내에서는 호출 가능)
    private UriBuilder withApiKey(UriBuilder uriBuilder) {
        if (apiKey != null && !apiKey.isBlank()) {
            uriBuilder.queryParam("api_key", apiKey);
        }
        return uriBuilder;
    }

    // "https://doi.org/10.xxx" → "10.xxx" (엑셀 데이터에 두 형식이 섞여 있을 수 있음)
    private String normalizeDoi(String doi) {
        if (doi == null) {
            return "";
        }
        String trimmed = doi.trim();
        if (trimmed.toLowerCase().startsWith(DOI_URL_PREFIX)) {
            return trimmed.substring(DOI_URL_PREFIX.length());
        }
        return trimmed;
    }
}
