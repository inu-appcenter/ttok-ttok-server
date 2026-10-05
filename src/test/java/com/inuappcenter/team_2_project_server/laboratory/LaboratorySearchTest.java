package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.LaboratoryResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchAreaCategory;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LaboratorySearchTest {

    @Autowired
    private LaboratoryService laboratoryService;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        ResearchAreaCategory ai = persist(ResearchAreaCategory.create("AI"));
        ResearchAreaCategory security = persist(ResearchAreaCategory.create("보안"));

        ResearchArea artificialIntelligence = area("인공지능", ai);
        ResearchArea computerVision = area("컴퓨터 비전", ai);
        ResearchArea cryptography = area("암호학", security);
        ResearchArea robotics = area("로보틱스", null);

        lab("인공지능 연구실", Department.COMPUTER_ENGINEERING, "홍길동", artificialIntelligence);
        lab("비전랩", Department.COMPUTER_ENGINEERING, "김철수", computerVision);
        lab("암호 연구실", Department.ELECTRONICS_ENGINEERING, "이영희", cryptography);
        lab("로봇 연구실", Department.MECHANICAL_ENGINEERING, "박민수", robotics);

        em.flush();
        em.clear();
    }

    @Test
    void returns_all_labs_when_no_condition() {
        assertThat(search(null, null, null, null))
                .containsExactly("로봇 연구실", "비전랩", "암호 연구실", "인공지능 연구실");
    }

    @Test
    void filters_by_single_category() {
        assertThat(search(null, List.of("AI"), null, null))
                .containsExactly("비전랩", "인공지능 연구실");
    }

    @Test
    void filters_by_multiple_categories_with_or() {
        assertThat(search(null, List.of("AI", "보안"), null, null))
                .containsExactly("비전랩", "암호 연구실", "인공지능 연구실");
    }

    @Test
    void ignores_blank_and_duplicated_categories() {
        assertThat(search(null, Arrays.asList(" AI ", "", "  ", null, "AI"), null, null))
                .containsExactly("비전랩", "인공지능 연구실");
    }

    @Test
    void returns_empty_when_category_does_not_match_department() {
        assertThat(search(null, List.of("AI"), null, "전자공학부")).isEmpty();
    }

    @Test
    void filters_by_college_and_department() {
        assertThat(search(null, null, "정보기술대학", null))
                .containsExactly("비전랩", "인공지능 연구실");
        assertThat(search(null, null, null, " 전자공학부 "))
                .containsExactly("암호 연구실");
    }

    @Test
    void keyword_matches_professor_name_ignoring_whitespace() {
        assertThat(search("  홍 길동 ", null, null, null))
                .containsExactly("인공지능 연구실");
    }

    @Test
    void keyword_matches_lab_name_ignoring_whitespace() {
        assertThat(search("인공지능연구실", null, null, null))
                .containsExactly("인공지능 연구실");
    }

    @Test
    void keyword_matches_research_area_name() {
        assertThat(search("컴퓨터비전", null, null, null))
                .containsExactly("비전랩");
    }

    @Test
    void keyword_matches_category_name() {
        assertThat(search("보안", null, null, null))
                .containsExactly("암호 연구실");
    }

    @Test
    void keyword_matches_department_name() {
        assertThat(search("기계", null, null, null))
                .containsExactly("로봇 연구실");
    }

    @Test
    void keyword_is_searched_only_within_filters() {
        assertThat(search("이영희", null, null, "컴퓨터공학부")).isEmpty();
        assertThat(search("김철수", List.of("AI"), "정보기술대학", "컴퓨터공학부"))
                .containsExactly("비전랩");
    }

    @Test
    void blank_keyword_means_all() {
        assertThat(search("   ", null, null, null)).hasSize(4);
    }

    @Test
    void like_wildcards_in_keyword_are_escaped() {
        assertThat(search("%", null, null, null)).isEmpty();
        assertThat(search("_", null, null, null)).isEmpty();
    }

    @Test
    void throws_when_department_name_does_not_exist() {
        assertThatThrownBy(() -> search(null, null, null, "없는학과"))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private List<String> search(String keyword, List<String> categories, String college, String department) {
        return laboratoryService.searchLabs(
                        keyword, categories, college, department,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "labName").and(Sort.by("id")))
                )
                .map(LaboratoryResponseDto::labName)
                .getContent();
    }

    private ResearchArea area(String name, ResearchAreaCategory category) {
        ResearchArea area = ResearchArea.create(name);
        if (category != null) {
            area.updateCategory(category);
        }
        return persist(area);
    }

    private void lab(String labName, Department department, String professorName, ResearchArea area) {
        Professor professor = persist(Professor.create(professorName, "교수", department, null, null));
        Laboratory laboratory = persist(Laboratory.create(
                department.getCollegeName(), department, labName, null, 0, 0, null, professor, null, area.getArea()
        ));
        persist(LaboratoryResearchArea.create(laboratory, area));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
