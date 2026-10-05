package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.client.NtisClient;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.NtisProjectSearchResponse;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ResearchProjectSyncService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 실제 DB에서 동기화 결과가 저장되는지 검증한다.
 * 테스트 메서드를 트랜잭션으로 감싸면 갱신 누락(detached 엔티티 변경이 저장되지 않는 문제)을 잡을 수 없어서,
 * 일부러 트랜잭션 없이 실행하고 만든 데이터는 직접 지운다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResearchProjectSyncIntegrationTest {

    private static final String FUTURE = "2099-12-31 00:00:00.0";
    private static final String PAST = "2000-01-01 00:00:00.0";

    @Autowired
    private ResearchProjectSyncService researchProjectSyncService;

    @Autowired
    private ResearchProjectRepository researchProjectRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @MockitoBean
    private NtisClient ntisClient;

    private final List<Laboratory> laboratories = new ArrayList<>();
    private final List<Professor> professors = new ArrayList<>();

    @AfterEach
    void tearDown() {
        researchProjectRepository.deleteAll();
        laboratoryRepository.deleteAll(laboratories);
        professorRepository.deleteAll(professors);
    }

    @Test
    void resync_persists_updated_values_of_existing_project() {
        laboratory(professor("동기화교수", Department.COMPUTER_ENGINEERING), "동기화연구실");
        given(ntisClient.searchByManagerName(anyString(), anyInt(), anyInt())).willReturn(List.of());
        given(ntisClient.searchByManagerName(eq("동기화교수"), anyInt(), anyInt()))
                .willReturn(List.of(hit("9100000001", "동기화교수", FUTURE)));
        researchProjectSyncService.syncAll();
        assertThat(findProject("9100000001").isOngoing()).isTrue();

        // 과제가 종료되어 NTIS 총연구기간 종료일이 과거가 된 상황
        given(ntisClient.searchByManagerName(eq("동기화교수"), anyInt(), anyInt()))
                .willReturn(List.of(hit("9100000001", "동기화교수", PAST)));
        researchProjectSyncService.syncAll();

        assertThat(findProject("9100000001").isOngoing()).isFalse();
    }

    @Test
    void same_name_projects_are_assigned_by_science_class_and_ambiguous_ones_are_held() {
        laboratory(professor("원자력교수", Department.SAFETY_ENGINEERING), "원자로연구실");
        laboratory(professor("건설교수", Department.URBAN_ARCHITECTURE_ENGINEERING), "건설연구실");
        Laboratory nuclearLab = laboratory(professor("동명교수", Department.SAFETY_ENGINEERING), "원자력안전연구실");
        Laboratory constructionLab = laboratory(professor("동명교수", Department.URBAN_ARCHITECTURE_ENGINEERING), "건설경영연구실");

        given(ntisClient.searchByManagerName(anyString(), anyInt(), anyInt())).willReturn(List.of());
        given(ntisClient.searchByManagerName(eq("원자력교수"), anyInt(), anyInt()))
                .willReturn(List.of(hit("9200000001", "원자력교수", PAST, "원자력")));
        given(ntisClient.searchByManagerName(eq("건설교수"), anyInt(), anyInt()))
                .willReturn(List.of(hit("9200000002", "건설교수", PAST, "건설/교통")));
        given(ntisClient.searchByManagerName(eq("동명교수"), anyInt(), anyInt()))
                .willReturn(List.of(
                        hit("9200000003", "동명교수", PAST, "원자력"),
                        hit("9200000004", "동명교수", PAST, "건설/교통"),
                        hit("9200000005", "동명교수", PAST, "에너지/자원")
                ));

        researchProjectSyncService.syncAll();

        assertThat(projectNumbersOf(nuclearLab)).containsExactly("9200000003");
        assertThat(projectNumbersOf(constructionLab)).containsExactly("9200000004");
        assertThat(researchProjectRepository.findByLaboratoryIsNull(PageRequest.of(0, 20)).getContent())
                .extracting(ResearchProject::getProjectNumber)
                .containsExactly("9200000005");
    }

    private ResearchProject findProject(String projectNumber) {
        return researchProjectRepository.findByProjectNumber(projectNumber).orElseThrow();
    }

    private List<String> projectNumbersOf(Laboratory laboratory) {
        return researchProjectRepository.findByLaboratory(laboratory, PageRequest.of(0, 20)).getContent().stream()
                .map(ResearchProject::getProjectNumber)
                .toList();
    }

    private Professor professor(String name, Department department) {
        Professor professor = professorRepository.save(Professor.create(name, "교수", department, null, null));
        professors.add(professor);
        return professor;
    }

    private Laboratory laboratory(Professor professor, String labName) {
        Laboratory laboratory = laboratoryRepository.save(Laboratory.create(
                professor.getCollege(), professor.getDepartment(), labName, null, 0, 0, null, professor, null, null
        ));
        laboratories.add(laboratory);
        return laboratory;
    }

    private NtisProjectSearchResponse.Hit hit(String projectNumber, String managerName, String totalEnd, String... scienceClassLarges) {
        List<NtisProjectSearchResponse.ScienceClass> scienceClasses = new ArrayList<>();
        for (int i = 0; i < scienceClassLarges.length; i++) {
            scienceClasses.add(new NtisProjectSearchResponse.ScienceClass("new", String.valueOf(i + 1), scienceClassLarges[i], null, null));
        }
        return new NtisProjectSearchResponse.Hit(
                projectNumber,
                new NtisProjectSearchResponse.ProjectTitle("국문 과제명", null),
                new NtisProjectSearchResponse.NamedEntity(managerName),
                null,
                null,
                new NtisProjectSearchResponse.NamedEntity("인천대학교"),
                null,
                null,
                "2024",
                new NtisProjectSearchResponse.ProjectPeriod("20240101", "20241231", "2024-01-01 00:00:00.0", totalEnd),
                null,
                null,
                scienceClasses
        );
    }
}
