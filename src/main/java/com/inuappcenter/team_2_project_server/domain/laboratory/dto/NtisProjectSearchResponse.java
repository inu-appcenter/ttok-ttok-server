package com.inuappcenter.team_2_project_server.domain.laboratory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Comparator;
import java.util.List;

/**
 * NTIS 국가R&D 과제검색 서비스(전체용) 응답 중, 대외용으로 제공 가능한 필드만 매핑한다.
 * 단, ScienceClass(과학기술표준분류)는 동명이인 교수의 과제를 어느 연구실에 매핑할지 판단하는 용도로만 받고,
 * DB에 저장하거나 응답으로 노출하지 않는다. 그 외 필드(NAVIGATION 등)는 매핑하지 않고 그대로 무시한다.
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
            String totalFunds,

            // 매핑 판단용 (저장/노출하지 않음)
            @JacksonXmlProperty(localName = "ScienceClass")
            @JacksonXmlElementWrapper(useWrapping = false)
            List<ScienceClass> scienceClasses
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

        // 신분류(type="new") 대분류를 sequence(1~3) 순서대로 반환. 구분류(type="old")는 실제 응답에서 비어 있어 쓰지 않는다
        public List<String> newScienceClassLarges() {
            if (scienceClasses == null) {
                return List.of();
            }
            return scienceClasses.stream()
                    .filter(scienceClass -> "new".equals(scienceClass.type()))
                    .filter(scienceClass -> scienceClass.large() != null && !scienceClass.large().isBlank())
                    .sorted(Comparator.comparingInt(ScienceClass::sequenceOrMax))
                    .map(scienceClass -> scienceClass.large().trim())
                    .toList();
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

    // 과학기술표준분류. 예) <ScienceClass type="new" sequence="1"><Large>원자력</Large><Medium>원자력안전기술</Medium><Small>...</Small></ScienceClass>
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScienceClass(
            @JacksonXmlProperty(isAttribute = true, localName = "type") String type,
            @JacksonXmlProperty(isAttribute = true, localName = "sequence") String sequence,
            @JacksonXmlProperty(localName = "Large") String large,
            @JacksonXmlProperty(localName = "Medium") String medium,
            @JacksonXmlProperty(localName = "Small") String small
    ) {
        private int sequenceOrMax() {
            try {
                return Integer.parseInt(sequence);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
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
