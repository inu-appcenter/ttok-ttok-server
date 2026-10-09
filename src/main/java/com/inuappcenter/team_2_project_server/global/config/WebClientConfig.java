package com.inuappcenter.team_2_project_server.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * 연구과제 API
     * NTIS가 응답하지 않을 때 스레드가 무한 대기하지 않도록 연결/응답 타임아웃을 명시적으로 건다
     * (연구실 231개를 순차로 조회하는 배치/수동 동기화 특성상, 하나만 걸려도 전체가 멈추면 안 된다)
     */
    @Bean
    public WebClient ntisWebClient(
            @Value("${ntis.base-url}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 연구 지표 API (OpenAlex)
     * 지표 동기화 배치에서 교수 수백 명을 순차로 조회하므로 NTIS와 같은 이유로 타임아웃을 건다
     * base-url은 고정값이라 yml에 없으면 기본값을 사용한다
     */
    @Bean
    public WebClient openAlexWebClient(
            @Value("${openalex.base-url}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // 응답 XML을 문자열로 받아 직접 파싱할 때 쓴다. WebClient의 자동 XML 디코딩에 기대지 않고
    // 우리가 정의한 필드(대외용 제공 가능 항목)만 매핑하며, 나머지 필드는 무시한다
    @Bean
    public XmlMapper ntisXmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper;
    }

    /**
     * AI 게이트웨이 API (연구실 추천: 임베딩, LLM 선택)
     * 아래 챗봇 API와 같은 키를 쓰지만, 추천은 사용자가 화면에서 기다리는 요청이라 타임아웃을 건다.
     * (응답이 늦으면 끊고 검색 결과만으로 대체 응답을 준다. 챗봇 API는 응답에 1~2분 걸리기도 해서 그대로 둔다)
     */
    @Bean
    public WebClient factChatGatewayWebClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 임베딩 응답은 문장 100개 x 숫자 1536개라 기본 버퍼(256KB)를 넘는다
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * AI 챗봇 API
     */
    @Bean
    public WebClient factChatWebClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
