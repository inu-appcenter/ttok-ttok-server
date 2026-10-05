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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ResearchProjectSyncServiceTest {

    private static final String FUTURE = "2099-12-31 00:00:00.0";
    private static final String PAST = "2000-01-01 00:00:00.0";

    private LaboratoryRepository laboratoryRepository;
    private ProfessorRepository professorRepository;
    private ResearchProjectRepository researchProjectRepository;
    private NtisClient ntisClient;
    private ResearchProjectSyncService researchProjectSyncService;

    private final List<Laboratory> laboratories = new ArrayList<>();
    private final List<Professor> professors = new ArrayList<>();
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        professorRepository = mock(ProfessorRepository.class);
        researchProjectRepository = mock(ResearchProjectRepository.class);
        ntisClient = mock(NtisClient.class);
        researchProjectSyncService = new ResearchProjectSyncService(
                laboratoryRepository,
                professorRepository,
                researchProjectRepository,
                ntisClient,
                new TransactionTemplate(mock(PlatformTransactionManager.class))
        );

        given(laboratoryRepository.findAll()).willReturn(laboratories);
        given(professorRepository.findAll()).willReturn(professors);
        given(researchProjectRepository.findByProjectNumber(anyString())).willReturn(Optional.empty());
        given(ntisClient.searchByManagerName(anyString(), anyInt(), anyInt())).willReturn(List.of());
    }

    // ===== 동명이인이 아닌 교수 =====

    @Test
    void creates_new_research_project_linked_to_professors_laboratory() {
        Laboratory laboratory = laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        ntisReturns("홍길동", hit("1711041912", "홍길동", PAST));

        researchProjectSyncService.syncAll();

        assertThat(savedLaboratoryByProjectNumber()).containsEntry("1711041912", laboratory);
    }

    @Test
    void updates_existing_research_project_without_saving_again() {
        Laboratory laboratory = laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        ResearchProject existing = mock(ResearchProject.class);
        given(researchProjectRepository.findByProjectNumber("1711041912")).willReturn(Optional.of(existing));
        ntisReturns("홍길동", hit("1711041912", "홍길동", PAST));

        researchProjectSyncService.syncAll();

        verify(existing).updateFromNtis(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()
        );
        verify(existing).assignLaboratory(laboratory);
        verify(researchProjectRepository, never()).save(any(ResearchProject.class));
    }

    @Test
    void skips_laboratory_without_professor() {
        laboratories.add(Laboratory.create(null, null, "교수없는연구실", null, 0, 0, null, null, null, null));

        researchProjectSyncService.syncAll();

        verify(ntisClient, never()).searchByManagerName(any(), anyInt(), anyInt());
    }

    @Test
    void marks_ongoing_by_total_period_end() {
        laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        ntisReturns("홍길동", hit("1000000001", "홍길동", FUTURE), hit("1000000002", "홍길동", PAST));

        researchProjectSyncService.syncAll();

        Map<String, ResearchProject> saved = savedByProjectNumber();
        assertThat(saved.get("1000000001").isOngoing()).isTrue();
        assertThat(saved.get("1000000002").isOngoing()).isFalse();
    }

    @Test
    void strips_ntis_search_word_highlight_markup() {
        laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        ntisReturns("홍길동", hit("1711041912", "<span class=\"search_word\">홍길동</span>", FUTURE));

        researchProjectSyncService.syncAll();

        ResearchProject saved = savedByProjectNumber().get("1711041912");
        assertThat(saved.getManagerName()).isEqualTo("홍길동");
        assertThat(saved.getResearchAgencyName()).isEqualTo("인천대학교산학협력단");
    }

    @Test
    void skips_project_whose_manager_name_is_not_exactly_the_professor_name() {
        laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        ntisReturns("홍길동", hit("1000000001", "홍길동", PAST), hit("1000000002", "홍길동수", PAST));

        researchProjectSyncService.syncAll();

        assertThat(savedByProjectNumber()).containsOnlyKeys("1000000001");
    }

    @Test
    void continues_with_other_professors_when_one_fails() {
        laboratory(professor("홍길동", Department.COMPUTER_ENGINEERING), "AI연구실", null);
        Laboratory other = laboratory(professor("이순신", Department.MECHANICAL_ENGINEERING), "기계연구실", null);
        ntisReturns("홍길동", hit("1000000001", "홍길동", PAST));
        ntisReturns("이순신", hit("1000000002", "이순신", PAST));
        given(researchProjectRepository.findByProjectNumber("1000000001")).willThrow(new IllegalStateException("DB 오류"));

        researchProjectSyncService.syncAll();

        assertThat(savedLaboratoryByProjectNumber()).containsOnlyKeys("1000000002").containsEntry("1000000002", other);
    }

    // ===== 동명이인 교수 =====

    @Test
    void assigns_same_name_professors_projects_by_science_class() {
        // 학과별 분류 분포를 만들기 위한, 동명이인이 아닌 같은 학과 교수들
        laboratory(professor("이원자", Department.SAFETY_ENGINEERING), "원자로연구실", null);
        laboratory(professor("박건설", Department.URBAN_ARCHITECTURE_ENGINEERING), "건설연구실", null);
        ntisReturns("이원자", hit("2000000001", "이원자", PAST, "원자력"), hit("2000000002", "이원자", PAST, "원자력"));
        ntisReturns("박건설", hit("2000000003", "박건설", PAST, "건설/교통"), hit("2000000004", "박건설", PAST, "건설/교통"));

        Laboratory nuclearLab = laboratory(professor("김태완", Department.SAFETY_ENGINEERING), "원자력안전연구실", null);
        Laboratory constructionLab = laboratory(professor("김태완", Department.URBAN_ARCHITECTURE_ENGINEERING), "건설경영및관리연구실", null);
        ntisReturns("김태완",
                hit("3000000001", "김태완", PAST, "원자력"),
                hit("3000000002", "김태완", PAST, "건설/교통", "정보/통신"),
                hit("3000000003", "김태완", PAST, "에너지/자원")
        );

        researchProjectSyncService.syncAll();

        Map<String, Laboratory> saved = savedLaboratoryByProjectNumber();
        assertThat(saved.get("3000000001")).isEqualTo(nuclearLab);
        assertThat(saved.get("3000000002")).isEqualTo(constructionLab);
        // 두 학과 어느 쪽 분포에도 없는 분류 → 판별 보류(연구실 없이 저장)
        assertThat(saved).containsKey("3000000003");
        assertThat(saved.get("3000000003")).isNull();
    }

    @Test
    void uses_laboratory_research_area_when_science_class_is_not_decisive() {
        Laboratory visionLab = laboratory(professor("김민수", Department.COMPUTER_ENGINEERING), "비전연구실", "컴퓨터 비전, 영상처리");
        laboratory(professor("김민수", Department.ELECTRONICS_ENGINEERING), "회로연구실", "아날로그 회로");
        ntisReturns("김민수", titledHit("4000000001", "김민수", "딥러닝 기반 컴퓨터비전 영상처리 기술"));

        researchProjectSyncService.syncAll();

        assertThat(savedLaboratoryByProjectNumber()).containsEntry("4000000001", visionLab);
    }

    @Test
    void does_not_save_project_of_same_name_professor_without_laboratory() {
        laboratory(professor("최화학", Department.CHEMISTRY), "유기합성연구실", null);
        laboratory(professor("정건축", Department.URBAN_ARCHITECTURE_ARCHITECTURE), "건축설계연구실", null);
        ntisReturns("최화학", hit("5000000001", "최화학", PAST, "화학"), hit("5000000002", "최화학", PAST, "화학"));
        ntisReturns("정건축", hit("5000000003", "정건축", PAST, "건설/교통"), hit("5000000004", "정건축", PAST, "건설/교통"));

        Laboratory chemistryLab = laboratory(professor("김진호", Department.CHEMISTRY), "촉매반응 및 유기합성 실험실", null);
        professor("김진호", Department.URBAN_ARCHITECTURE_ARCHITECTURE); // 연구실 없는 동명이인
        ntisReturns("김진호",
                hit("6000000001", "김진호", PAST, "화학"),
                hit("6000000002", "김진호", PAST, "건설/교통")
        );
        // 이전 동기화에서 잘못 저장된 과제는 정리, 관리자가 직접 지정한 과제는 유지
        ResearchProject wronglySaved = ResearchProject.create(chemistryLab, "6000000002", null, null, "김진호",
                null, null, null, null, null, null, null, null, null, null, null, null, null, false);
        given(researchProjectRepository.findByProjectNumber("6000000002")).willReturn(Optional.of(wronglySaved));

        researchProjectSyncService.syncAll();

        Map<String, Laboratory> saved = savedLaboratoryByProjectNumber();
        assertThat(saved.get("6000000001")).isEqualTo(chemistryLab);
        assertThat(saved).doesNotContainKey("6000000002");
        verify(researchProjectRepository).delete(wronglySaved);
    }

    @Test
    void keeps_manually_assigned_project_of_same_name_professor_without_laboratory() {
        laboratory(professor("최화학", Department.CHEMISTRY), "유기합성연구실", null);
        laboratory(professor("정건축", Department.URBAN_ARCHITECTURE_ARCHITECTURE), "건축설계연구실", null);
        ntisReturns("최화학", hit("5000000001", "최화학", PAST, "화학"));
        ntisReturns("정건축", hit("5000000003", "정건축", PAST, "건설/교통"));

        Laboratory chemistryLab = laboratory(professor("김진호", Department.CHEMISTRY), "촉매반응 및 유기합성 실험실", null);
        professor("김진호", Department.URBAN_ARCHITECTURE_ARCHITECTURE);
        ntisReturns("김진호", hit("6000000002", "김진호", PAST, "건설/교통"));
        ResearchProject manuallyAssigned = ResearchProject.create(null, "6000000002", null, null, "김진호",
                null, null, null, null, null, null, null, null, null, null, null, null, null, false);
        manuallyAssigned.assignLaboratoryManually(chemistryLab);
        given(researchProjectRepository.findByProjectNumber("6000000002")).willReturn(Optional.of(manuallyAssigned));

        researchProjectSyncService.syncAll();

        verify(researchProjectRepository, never()).delete(any(ResearchProject.class));
        assertThat(manuallyAssigned.getLaboratory()).isEqualTo(chemistryLab);
    }

    @Test
    void auto_mapping_does_not_overwrite_manual_assignment() {
        Laboratory manualLab = mock(Laboratory.class);
        Laboratory autoLab = mock(Laboratory.class);
        ResearchProject project = ResearchProject.create(null, "7000000001", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, false);

        project.assignLaboratoryManually(manualLab);
        project.assignLaboratory(autoLab);
        project.assignLaboratory(null);

        assertThat(project.getLaboratory()).isEqualTo(manualLab);
        assertThat(project.isManuallyAssigned()).isTrue();
    }

    // ===== helpers =====

    private Professor professor(String name, Department department) {
        Professor professor = Professor.create(name, "교수", department, null, null);
        ReflectionTestUtils.setField(professor, "id", nextId++);
        professors.add(professor);
        return professor;
    }

    private Laboratory laboratory(Professor professor, String labName, String researchFieldRaw) {
        Laboratory laboratory = Laboratory.create(
                professor.getCollege(), professor.getDepartment(), labName, null, 0, 0, null, professor, null, researchFieldRaw
        );
        ReflectionTestUtils.setField(laboratory, "id", nextId++);
        laboratories.add(laboratory);
        return laboratory;
    }

    private void ntisReturns(String name, NtisProjectSearchResponse.Hit... hits) {
        given(ntisClient.searchByManagerName(eq(name), anyInt(), anyInt())).willReturn(List.of(hits));
    }

    private NtisProjectSearchResponse.Hit hit(String projectNumber, String managerName, String totalEnd, String... scienceClassLarges) {
        return hit(projectNumber, managerName, totalEnd, "국문 과제명", List.of(scienceClassLarges));
    }

    // 과학기술표준분류 없이 과제명만 있는 과제
    private NtisProjectSearchResponse.Hit titledHit(String projectNumber, String managerName, String title) {
        return hit(projectNumber, managerName, PAST, title, List.of());
    }

    private NtisProjectSearchResponse.Hit hit(String projectNumber, String managerName, String totalEnd, String title, List<String> scienceClassLarges) {
        List<NtisProjectSearchResponse.ScienceClass> scienceClasses = new ArrayList<>();
        scienceClasses.add(new NtisProjectSearchResponse.ScienceClass("old", null, null, null, null));
        for (int i = 0; i < scienceClassLarges.size(); i++) {
            scienceClasses.add(new NtisProjectSearchResponse.ScienceClass("new", String.valueOf(i + 1), scienceClassLarges.get(i), null, null));
        }

        return new NtisProjectSearchResponse.Hit(
                projectNumber,
                new NtisProjectSearchResponse.ProjectTitle(title, "English Title"),
                new NtisProjectSearchResponse.NamedEntity(managerName),
                new NtisProjectSearchResponse.TextBlock("연구내용 전문", null),
                new NtisProjectSearchResponse.Keyword("키워드1,키워드2", "keyword1,keyword2"),
                new NtisProjectSearchResponse.NamedEntity("<span class=\"search_word\">인천대학교</span>산학협력단"),
                new NtisProjectSearchResponse.NamedEntity("예산사업명"),
                new NtisProjectSearchResponse.NamedEntity("과학기술정보통신부"),
                "2024",
                new NtisProjectSearchResponse.ProjectPeriod("20240101", "20241231", "2024-01-01 00:00:00.0", totalEnd),
                "100000000",
                "150000000",
                scienceClasses
        );
    }

    private Map<String, ResearchProject> savedByProjectNumber() {
        ArgumentCaptor<ResearchProject> captor = ArgumentCaptor.forClass(ResearchProject.class);
        verify(researchProjectRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .collect(Collectors.toMap(ResearchProject::getProjectNumber, Function.identity()));
    }

    // 값이 null(매핑 보류)일 수 있어 Collectors.toMap 대신 직접 담는다
    private Map<String, Laboratory> savedLaboratoryByProjectNumber() {
        Map<String, Laboratory> result = new HashMap<>();
        savedByProjectNumber().forEach((projectNumber, project) -> result.put(projectNumber, project.getLaboratory()));
        return result;
    }
}
