package com.inuappcenter.team_2_project_server.domain.laboratory.client;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTIS 국가R&D 과제검색 서비스(전체용) 연동. 서비스 대상이 인천대학교 하나뿐이라
 * 과제수행기관(PB01)은 인천대학교로 고정하고, 연구책임자명(searchFd=AU)으로만 검색한다.
 */
@Slf4j
@Component
public class NtisClient {

    private static final String SEARCH_PATH = "/rndopen/openApi/public_project";
    private static final String FIXED_INSTITUTION_QUERY = "PB01=인천대학교";

    // NTIS는 인증키 오류, IP 미허용, 요청 제한 초과 같은 상황도 HTTP 200과 함께
    // <error>메시지</error> 형태의 본문으로 내려준다. 이 형태를 못 잡으면 그냥 "결과 0건"으로
    // 조용히 넘어가버려서 원인 파악이 불가능해지므로, 파싱 전에 먼저 이 envelope인지 확인한다
    private static final Pattern ERROR_ENVELOPE = Pattern.compile("<error>(.*?)</error>", Pattern.DOTALL);

    private final WebClient ntisWebClient;
    private final XmlMapper ntisXmlMapper;
    private final String apiKey;

    public NtisClient(
            @Qualifier("ntisWebClient") WebClient ntisWebClient,
            XmlMapper ntisXmlMapper,
            @Value("${ntis.api-key}") String apiKey
    ) {
        this.ntisWebClient = ntisWebClient;
        this.ntisXmlMapper = ntisXmlMapper;
        this.apiKey = apiKey;
    }

    /**
     * 담당교수 이름(연구책임자명)으로 인천대학교 소속 국가R&D 과제를 검색한다.
     * NTIS 연동 자체가 실패해도(장애/레이트리밋 등) 배치 전체가 죽으면 안 되므로,
     * 여기서 예외를 잡아 로그만 남기고 빈 리스트를 반환한다.
     */
    public List<NtisProjectSearchResponse.Hit> searchByManagerName(
            String managerName,
            int startPosition,
            int displayCount
    ) {
        try {
            String xml = ntisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_PATH)
                            .queryParam("apprvKey", apiKey)
                            .queryParam("collection", "project")
                            .queryParam("SRWR", managerName)
                            .queryParam("searchFd", "AU")
                            .queryParam("addQuery", FIXED_INSTITUTION_QUERY)
                            .queryParam("searchRnkn", "DATE/DESC")
                            .queryParam("startPosition", startPosition)
                            .queryParam("displayCnt", displayCount)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (xml == null || xml.isBlank()) {
                return List.of();
            }

            Matcher errorMatcher = ERROR_ENVELOPE.matcher(xml);
            if (errorMatcher.find()) {
                log.error("NTIS API 오류 응답 (managerName={}): {}", managerName, errorMatcher.group(1).trim());
                return List.of();
            }

            NtisProjectSearchResponse response = ntisXmlMapper.readValue(xml, NtisProjectSearchResponse.class);
            return response.hitsOrEmpty();
        } catch (Exception e) {
            log.error("NTIS 과제검색 실패 (managerName={})", managerName, e);
            return List.of();
        }
    }
}
