package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.client.NtisClient;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchProjectSyncService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class ResearchProjectSyncServiceTest {

    private LaboratoryRepository laboratoryRepository;
    private ResearchProjectRepository researchProjectRepository;
    private NtisClient ntisClient;
    private ResearchProjectSyncService researchProjectSyncService;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        researchProjectRepository = mock(ResearchProjectRepository.class);
        ntisClient = mock(NtisClient.class);
        researchProjectSyncService = new ResearchProjectSyncService(laboratoryRepository, researchProjectRepository, ntisClient);
    }

    @Test
    void syncOne_creates_new_research_project_when_not_found() {
        Laboratory laboratory = laboratory();
        given(ntisClient.searchByManagerName(eq("홍길동"), anyInt(), anyInt()))
                .willReturn(List.of(hit("1711041912", "2020-06-30 00:00:00.0")));
        given(researchProjectRepository.findByProjectNumber("1711041912")).willReturn(Optional.empty());

        researchProjectSyncService.syncOne(laboratory);

        verify(researchProjectRepository).save(any(ResearchProject.class));
    }

    @Test
    void syncOne_updates_existing_research_project_when_found() {
        Laboratory laboratory = laboratory();
        ResearchProject existing = mock(ResearchProject.class);
        given(ntisClient.searchByManagerName(eq("홍길동"), anyInt(), anyInt()))
                .willReturn(List.of(hit("1711041912", "2020-06-30 00:00:00.0")));
        given(researchProjectRepository.findByProjectNumber("1711041912")).willReturn(Optional.of(existing));

        researchProjectSyncService.syncOne(laboratory);

        verify(existing).updateFromNtis(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()
        );
        verify(researchProjectRepository, never()).save(any(ResearchProject.class));
    }

    @Test
    void syncOne_skips_laboratory_without_professor() {
        Laboratory laboratory = mock(Laboratory.class);
        given(laboratory.getProfessor()).willReturn(null);

        researchProjectSyncService.syncOne(laboratory);

        verify(ntisClient, never()).searchByManagerName(any(), anyInt(), anyInt());
    }

    @Test
    void syncOne_marks_ongoing_true_when_total_end_is_in_the_future() {
        Laboratory laboratory = laboratory();
        given(ntisClient.searchByManagerName(eq("홍길동"), anyInt(), anyInt()))
                .willReturn(List.of(hit("1711041912", "2099-12-31 00:00:00.0")));
        given(researchProjectRepository.findByProjectNumber("1711041912")).willReturn(Optional.empty());

        researchProjectSyncService.syncOne(laboratory);

        verify(researchProjectRepository).save(argThatOngoing(true));
    }

    @Test
    void syncOne_marks_ongoing_false_when_total_end_is_in_the_past() {
        Laboratory laboratory = laboratory();
        given(ntisClient.searchByManagerName(eq("홍길동"), anyInt(), anyInt()))
                .willReturn(List.of(hit("1711041912", "2000-01-01 00:00:00.0")));
        given(researchProjectRepository.findByProjectNumber("1711041912")).willReturn(Optional.empty());

        researchProjectSyncService.syncOne(laboratory);

        verify(researchProjectRepository).save(argThatOngoing(false));
    }

    private ResearchProject argThatOngoing(boolean expected) {
        return org.mockito.ArgumentMatchers.argThat(rp -> rp.isOngoing() == expected);
    }

    private Laboratory laboratory() {
        Professor professor = Professor.create(
                "홍길동", "교수", Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
        );
        return Laboratory.create(
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING,
                "AI연구실",
                "7호관 401호",
                6,
                7,
                null,
                professor,
                "https://lab.example.com",
                null
        );
    }

    private NtisProjectSearchResponse.Hit hit(String projectNumber, String totalEnd) {
        return new NtisProjectSearchResponse.Hit(
                projectNumber,
                new NtisProjectSearchResponse.ProjectTitle("국문 과제명", "English Title"),
                new NtisProjectSearchResponse.NamedEntity("홍길동"),
                new NtisProjectSearchResponse.TextBlock("연구내용 전문", null),
                new NtisProjectSearchResponse.Keyword("키워드1,키워드2", "keyword1,keyword2"),
                new NtisProjectSearchResponse.NamedEntity("인천대학교"),
                new NtisProjectSearchResponse.NamedEntity("예산사업명"),
                new NtisProjectSearchResponse.NamedEntity("과학기술정보통신부"),
                "2024",
                new NtisProjectSearchResponse.ProjectPeriod("20240101", "20241231", "2024-01-01 00:00:00.0", totalEnd),
                "100000000",
                "150000000"
        );
    }
}
