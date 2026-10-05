package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.ResearchProjectResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchProject;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchProjectRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class LaboratoryServiceResearchProjectTest {

    private LaboratoryRepository laboratoryRepository;
    private ResearchProjectRepository researchProjectRepository;
    private LaboratoryService laboratoryService;

    @BeforeEach
    void setUp() {
        laboratoryRepository = mock(LaboratoryRepository.class);
        researchProjectRepository = mock(ResearchProjectRepository.class);
        laboratoryService = new LaboratoryService(
                laboratoryRepository,
                mock(ProfessorService.class),
                mock(PublicationRepository.class),
                researchProjectRepository,
                mock(ResearcherService.class)
        );
    }

    @Test
    void returns_pending_research_projects() {
        Pageable pageable = PageRequest.of(0, 20);
        given(researchProjectRepository.findByLaboratoryIsNull(pageable))
                .willReturn(new PageImpl<>(List.of(project(null)), pageable, 1));

        Page<ResearchProjectResponseDto> result = laboratoryService.getPendingResearchProjects(pageable);

        assertThat(result.getContent()).extracting(ResearchProjectResponseDto::projectNumber).containsExactly("1711041912");
    }

    @Test
    void assigns_laboratory_manually() {
        ResearchProject project = project(null);
        Laboratory laboratory = mock(Laboratory.class);
        given(researchProjectRepository.findById(1L)).willReturn(Optional.of(project));
        given(laboratoryRepository.findById(10L)).willReturn(Optional.of(laboratory));

        ResearchProjectResponseDto response = laboratoryService.assignResearchProjectLaboratory(1L, 10L);

        assertThat(response.projectNumber()).isEqualTo("1711041912");
        assertThat(project.getLaboratory()).isEqualTo(laboratory);
        assertThat(project.isManuallyAssigned()).isTrue();
    }

    @Test
    void throws_when_research_project_does_not_exist() {
        given(researchProjectRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> laboratoryService.assignResearchProjectLaboratory(1L, 10L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESEARCH_PROJECT_NOT_FOUND);
    }

    @Test
    void throws_when_laboratory_does_not_exist() {
        ResearchProject project = project(null);
        given(researchProjectRepository.findById(1L)).willReturn(Optional.of(project));
        given(laboratoryRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> laboratoryService.assignResearchProjectLaboratory(1L, 10L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABORATORY_NOT_FOUND);
        assertThat(project.isManuallyAssigned()).isFalse();
    }

    private ResearchProject project(Laboratory laboratory) {
        return ResearchProject.create(laboratory, "1711041912", "과제명", null, "김태완",
                null, null, null, null, null, null, null, null, null, null, null, null, null, false);
    }
}
