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

    @Test
    void contentSummary_falls_back_to_full_when_teaser_is_blank() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NtisProjectSearchResponse response = xmlMapper.readValue(SAMPLE_XML, NtisProjectSearchResponse.class);
        NtisProjectSearchResponse.Hit hit = response.hitsOrEmpty().get(0);

        // 매뉴얼 예시에서도 Teaser는 비어있고 Full에 요약 분량이 들어온다
        assertThat(hit.contentSummary()).startsWith("나노 멤브레인 공정 플랫폼 개발");
    }
}
