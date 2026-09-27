package com.inuappcenter.team_2_project_server.domain.laboratory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * NTIS 국가R&D 과제검색 서비스(전체용) 응답 중, 대외용으로 제공 가능한 필드만 매핑한다.
 * 그 외 필드(NAVIGATION, ScienceClass 등)는 매핑하지 않고 그대로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESULT")
public record NtisProjectSearchResponse(
        @JacksonXmlProperty(localName = "TOTALHITS")
        Integer totalHits,

        @JacksonXmlProperty(localName = "RESULTSET")
        ResultSet resultSet
) {

    public List<Hit> hitsOrEmpty() {
        return resultSet == null || resultSet.hits() == null ? List.of() : resultSet.hits();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultSet(
            @JacksonXmlProperty(localName = "HIT")
            @JacksonXmlElementWrapper(useWrapping = false)
            List<Hit> hits
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hit(
            @JacksonXmlProperty(localName = "ProjectNumber")
            String projectNumber,

            @JacksonXmlProperty(localName = "ProjectTitle")
            ProjectTitle projectTitle,

            @JacksonXmlProperty(localName = "Manager")
            NamedEntity manager,

            @JacksonXmlProperty(localName = "Abstract")
            TextBlock abstractBlock,

            @JacksonXmlProperty(localName = "Keyword")
            Keyword keyword,

            @JacksonXmlProperty(localName = "ResearchAgency")
            NamedEntity researchAgency,

            @JacksonXmlProperty(localName = "BudgetProject")
            NamedEntity budgetProject,

            @JacksonXmlProperty(localName = "Ministry")
            NamedEntity ministry,

            @JacksonXmlProperty(localName = "ProjectYear")
            String projectYear,

            @JacksonXmlProperty(localName = "ProjectPeriod")
            ProjectPeriod projectPeriod,

            @JacksonXmlProperty(localName = "GovernmentFunds")
            String governmentFunds,

            @JacksonXmlProperty(localName = "TotalFunds")
            String totalFunds
    ) {
        // 연구내용 요약. Teaser(약 250자)를 우선 쓰고 없으면 Full로 대체
        // (실제 응답에서 Teaser가 비어있고 Full에 요약 분량만 들어오는 경우가 있어 방어적으로 처리)
        public String contentSummary() {
            if (abstractBlock == null) {
                return null;
            }
            if (abstractBlock.teaser() != null && !abstractBlock.teaser().isBlank()) {
                return abstractBlock.teaser();
            }
            return abstractBlock.full();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectTitle(
            @JacksonXmlProperty(localName = "Korean") String korean,
            @JacksonXmlProperty(localName = "English") String english
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedEntity(
            @JacksonXmlProperty(localName = "Name") String name
    ) {
    }

    // Goal/Abstract/Effect가 전부 Full·Teaser 구조를 공유하지만, 지금은 Abstract만 쓴다
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextBlock(
            @JacksonXmlProperty(localName = "Full") String full,
            @JacksonXmlProperty(localName = "Teaser") String teaser
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Keyword(
            @JacksonXmlProperty(localName = "Korean") String korean,
            @JacksonXmlProperty(localName = "English") String english
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectPeriod(
            @JacksonXmlProperty(localName = "Start") String start,
            @JacksonXmlProperty(localName = "End") String end,
            @JacksonXmlProperty(localName = "TotalStart") String totalStart,
            @JacksonXmlProperty(localName = "TotalEnd") String totalEnd
    ) {
    }
}
