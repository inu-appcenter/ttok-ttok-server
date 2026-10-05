package com.inuappcenter.team_2_project_server.laboratory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NTIS 오픈API 매뉴얼(국가R&D 과제검색 서비스 전체용, 2025)에 실린 실제 응답 예시를 기반으로,
 * 우리가 정의한 필드만 정확히 매핑되는지 검증한다. (NAVIGATION 등 나머지 필드는 무시되어야 함)
 */
class NtisProjectSearchResponseTest {

    private static final String SAMPLE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESULT>
                <NAVIGATION>
                    <NAVIGATIONENTRY NAME="leadagency">
                        <NAVIGATIONELEMENTS COUNT="1">
                            <NAVIGATIONELEMENT NAME="인천대학교" COUNT="1" />
                        </NAVIGATIONELEMENTS>
                    </NAVIGATIONENTRY>
                </NAVIGATION>
                <TOTALHITS>1</TOTALHITS>
                <STARTPOSITION>1</STARTPOSITION>
                <HITS>1</HITS>
                <RESULTSET>
                    <HIT NO="1">
                        <ProjectNumber>1711041912</ProjectNumber>
                        <ProjectTitle>
                            <Korean>스마트 센서 응용을 위한 나노 멤브레인 공정 플랫폼 개발</Korean>
                            <English>Platform development of nano membrane process for smart sensor application</English>
                        </ProjectTitle>
                        <Manager>
                            <Name>이종근</Name>
                        </Manager>
                        <Goal>
                            <Full>연구목표 전문...</Full>
                            <Teaser />
                        </Goal>
                        <Abstract>
                            <Full>나노 멤브레인 공정 플랫폼 개발 및 파운드리 서비스 - 전통적인 MEMS 공정기반...</Full>
                            <Teaser />
                        </Abstract>
                        <Keyword>
                            <Korean>나노 멤브레인,NEMS,가스 센서, 적외선 센서,공정플랫폼,마이크로 히터</Korean>
                            <English>Nanomembrane,NEMS,Gassensor,Infraredsensor,Processplatform,Microheater</English>
                        </Keyword>
                        <OrderAgency>
                            <Name>한국연구재단</Name>
                        </OrderAgency>
                        <ResearchAgency>
                            <Name>인천대학교</Name>
                        </ResearchAgency>
                        <BudgetProject>
                            <Name>나노·소재기술개발</Name>
                        </BudgetProject>
                        <Ministry>
                            <Name>과학기술정보통신부</Name>
                        </Ministry>
                        <ProjectYear>2016</ProjectYear>
                        <ProjectPeriod>
                            <Start>20160701</Start>
                            <End>20170630</End>
                            <TotalStart>2015-10-01 00:00:00.0</TotalStart>
                            <TotalEnd>2020-06-30 00:00:00.0</TotalEnd>
                        </ProjectPeriod>
                        <GovernmentFunds>300000000</GovernmentFunds>
                        <TotalFunds>475000000</TotalFunds>
                    </HIT>
                </RESULTSET>
            </RESULT>
            """;

    @Test
    void parses_allowed_fields_and_ignores_the_rest() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(SAMPLE_XML, NtisProjectSearchResponse.class);

        assertThat(response.totalHits()).isEqualTo(1);
        assertThat(response.hitsOrEmpty()).hasSize(1);

        NtisProjectSearchResponse.Hit hit = response.hitsOrEmpty().get(0);
        assertThat(hit.projectNumber()).isEqualTo("1711041912");
        assertThat(hit.projectTitle().korean()).isEqualTo("스마트 센서 응용을 위한 나노 멤브레인 공정 플랫폼 개발");
        assertThat(hit.manager().name()).isEqualTo("이종근");
        assertThat(hit.researchAgency().name()).isEqualTo("인천대학교");
        assertThat(hit.budgetProject().name()).isEqualTo("나노·소재기술개발");
        assertThat(hit.ministry().name()).isEqualTo("과학기술정보통신부");
        assertThat(hit.projectYear()).isEqualTo("2016");
        assertThat(hit.projectPeriod().start()).isEqualTo("20160701");
        assertThat(hit.projectPeriod().totalEnd()).isEqualTo("2020-06-30 00:00:00.0");
        assertThat(hit.governmentFunds()).isEqualTo("300000000");
        assertThat(hit.totalFunds()).isEqualTo("475000000");
        assertThat(hit.keyword().korean()).contains("나노 멤브레인");
    }

    // 실제 응답에서는 검색어와 일치하는 이름에 <span class="search_word">...</span> 하이라이트가
    // XML 엔티티로 이스케이프되어 들어온다 (예: 담당교수명으로 검색했을 때의 Manager.Name).
    private static final String REAL_WORLD_HIT_WITH_HIGHLIGHT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESULT>
                <TOTALHITS>1</TOTALHITS>
                <RESULTSET>
                    <HIT NO="1">
                        <ProjectNumber>2710114905</ProjectNumber>
                        <ProjectTitle>
                            <Korean>멀티모달 에이전틱 AI의 개인정보 유출 분석 및 방지를 위한 상호작용 기반 데이터 생성 및 평가 기술 개발</Korean>
                            <English>Development of Interaction-Driven Data Generation and Evaluation Techniques</English>
                        </ProjectTitle>
                        <Manager>
                            <Name>&lt;span class="search_word"&gt;이장호&lt;/span&gt;</Name>
                        </Manager>
                        <Abstract>
                            <Full>(1) 세부목표 #1: 멀티모달 정렬 불일치 기반 개인정보 유출 메커니즘 분석 및 데이터셋 구축</Full>
                            <Teaser>(1) 세부목표 #1: 멀티모달 정렬 불일치 기반 개인정보 유출 메커니즘 분석 및 데이터셋 구축</Teaser>
                        </Abstract>
                        <Keyword>
                            <Korean>멀티모달 에이전틱 AI,정렬 불일치</Korean>
                            <English>Multimodal Agentic AI,Alignment Misalignment</English>
                        </Keyword>
                        <ResearchAgency>
                            <Name>&lt;span class="search_word"&gt;인천대학교&lt;/span&gt;산학협력단</Name>
                        </ResearchAgency>
                        <BudgetProject>
                            <Name>개인기초연구(과기정통부)</Name>
                        </BudgetProject>
                        <Ministry>
                            <Name>과학기술정보통신부</Name>
                        </Ministry>
                        <ProjectYear>2026</ProjectYear>
                        <ProjectPeriod>
                            <Start>20260901</Start>
                            <End>20270831</End>
                            <TotalStart>2026-09-01 00:00:00.0</TotalStart>
                            <TotalEnd>2029-08-31 00:00:00.0</TotalEnd>
                        </ProjectPeriod>
                        <GovernmentFunds>46180000</GovernmentFunds>
                        <TotalFunds>46180000</TotalFunds>
                    </HIT>
                </RESULTSET>
            </RESULT>
            """;

    @Test
    void parses_real_world_response_even_when_manager_name_has_search_word_highlight_markup() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(REAL_WORLD_HIT_WITH_HIGHLIGHT_XML, NtisProjectSearchResponse.class);

        assertThat(response.hitsOrEmpty()).hasSize(1);
        NtisProjectSearchResponse.Hit hit = response.hitsOrEmpty().get(0);

        // 검색어가 매칭된 필드는 NTIS가 <span class="search_word">...</span>로 감싸서 내려준다.
        // 지금 구조는 이를 그대로(마크업 포함) 저장하므로, 실제 담당교수명과 다르게 저장될 수 있다.
        assertThat(hit.manager().name()).isEqualTo("<span class=\"search_word\">이장호</span>");
        assertThat(hit.projectNumber()).isEqualTo("2710114905");
    }

    @Test
    void contentSummary_falls_back_to_full_when_teaser_is_blank() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(SAMPLE_XML, NtisProjectSearchResponse.class);
        NtisProjectSearchResponse.Hit hit = response.hitsOrEmpty().get(0);

        // 매뉴얼 예시에서도 Teaser는 비어있고 Full에 요약 분량이 들어온다
        assertThat(hit.contentSummary()).startsWith("나노 멤브레인 공정 플랫폼 개발");
    }

    // 운영 서버에서 확인한 실제 응답 구조: 신분류(type="new")는 sequence 1~3, 구분류(type="old")는 비어 있음
    private static final String SCIENCE_CLASS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESULT>
                <RESULTSET>
                    <HIT NO="1">
                        <ProjectNumber>1711000001</ProjectNumber>
                        <ScienceClass type="old" />
                        <ScienceClass type="new" sequence="2">
                            <Large code="EA">원자력</Large>
                            <Medium>원자력안전기술</Medium>
                            <Small>설계기준사고 열수력 안전성 실증/평가기술</Small>
                        </ScienceClass>
                        <ScienceClass type="new" sequence="1">
                            <Large>건설/교통</Large>
                            <Medium>건설시공/재료</Medium>
                            <Small>건설시공관리기술</Small>
                        </ScienceClass>
                        <ScienceClass type="new" sequence="3">
                            <Large />
                        </ScienceClass>
                    </HIT>
                </RESULTSET>
            </RESULT>
            """;

    @Test
    void newScienceClassLarges_returns_new_classification_in_sequence_order() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(SCIENCE_CLASS_XML, NtisProjectSearchResponse.class);
        NtisProjectSearchResponse.Hit hit = response.hitsOrEmpty().get(0);

        assertThat(hit.scienceClasses()).hasSize(4);
        assertThat(hit.newScienceClassLarges()).containsExactly("건설/교통", "원자력");
    }

    @Test
    void newScienceClassLarges_is_empty_when_science_class_is_missing() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(SAMPLE_XML, NtisProjectSearchResponse.class);

        assertThat(response.hitsOrEmpty().get(0).newScienceClassLarges()).isEmpty();
    }
}
