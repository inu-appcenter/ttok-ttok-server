package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class LaboratoryServiceSearchTest {

    private final Pageable pageable = PageRequest.of(0, 20);
    private LaboratoryRepository laboratoryRepository;
    private LaboratoryService laboratoryService;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        laboratoryService = new LaboratoryService(
                laboratoryRepository,
                mock(ProfessorService.class),
                mock(ResearcherService.class)
        );
        given(laboratoryRepository.searchByFilter(anyBoolean(), anyList(), any(), any(), anyString(), anyList(), any()))
                .willReturn(Page.empty());
    }

    @Test
    void no_condition_searches_all() {
        laboratoryService.searchLabs(null, null, null, null, pageable);

        verify(laboratoryRepository).searchByFilter(true, List.of(), null, null, "", List.of(), pageable);
    }

    @Test
    void categories_are_trimmed_and_blank_or_duplicated_values_removed() {
        laboratoryService.searchLabs(null, Arrays.asList(" AI ", "", "  ", null, "AI", "보안"), null, null, pageable);

        verify(laboratoryRepository).searchByFilter(false, List.of("AI", "보안"), null, null, "", List.of(), pageable);
    }

    @Test
    void only_blank_categories_means_all() {
        laboratoryService.searchLabs(null, List.of(" ", ""), null, null, pageable);

        verify(laboratoryRepository).searchByFilter(true, List.of(), null, null, "", List.of(), pageable);
    }

    @Test
    void college_and_department_names_are_trimmed_and_converted() {
        laboratoryService.searchLabs(null, null, " 정보기술대학 ", " 컴퓨터공학부 ", pageable);

        verify(laboratoryRepository).searchByFilter(
                true, List.of(), College.COLLEGE_OF_INFORMATION_TECHNOLOGY, Department.COMPUTER_ENGINEERING, "", List.of(), pageable);
    }

    @Test
    void keyword_whitespace_is_removed_and_lowercased() {
        laboratoryService.searchLabs("  Deep  Learning\t랩 ", null, null, null, pageable);

        verify(laboratoryRepository).searchByFilter(true, List.of(), null, null, "deeplearning랩", List.of(), pageable);
    }

    @Test
    void blank_keyword_means_all() {
        laboratoryService.searchLabs("   ", null, null, null, pageable);

        verify(laboratoryRepository).searchByFilter(true, List.of(), null, null, "", List.of(), pageable);
    }

    @Test
    void like_wildcards_in_keyword_are_escaped() {
        laboratoryService.searchLabs("a%b_c\\", null, null, null, pageable);

        verify(laboratoryRepository).searchByFilter(true, List.of(), null, null, "a\\%b\\_c\\\\", List.of(), pageable);
    }

    @Test
    void keyword_finds_departments_by_korean_name_ignoring_whitespace() {
        laboratoryService.searchLabs("컴퓨터 공학", null, null, null, pageable);

        verify(laboratoryRepository).searchByFilter(
                true, List.of(), null, null, "컴퓨터공학", List.of(Department.COMPUTER_ENGINEERING), pageable);
    }

    @Test
    void throws_when_college_name_does_not_exist() {
        assertThatThrownBy(() -> laboratoryService.searchLabs(null, null, "없는대학", null, pageable))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(laboratoryRepository, never()).searchByFilter(anyBoolean(), anyList(), any(), any(), anyString(), anyList(), eq(pageable));
    }

    @Test
    void throws_when_department_name_does_not_exist() {
        assertThatThrownBy(() -> laboratoryService.searchLabs(null, null, null, "없는학과", pageable))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
