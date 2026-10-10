package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.LaboratoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.service.ProfessorService;
import com.inuappcenter.team_2_project_server.domain.member.service.ResearcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class LaboratoryServiceFindByProfessorTest {

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
    }

    @Test
    void findLabByProfessorId_returns_laboratory_of_professor() {
        given(laboratoryRepository.findByProfessorId(1L)).willReturn(Optional.of(laboratory()));

        Optional<LaboratoryResponseDto> result = laboratoryService.findLabByProfessorId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().labName()).isEqualTo("AI연구실");
        assertThat(result.get().professor().name()).isEqualTo("홍길동");
    }

    @Test
    void findLabByProfessorId_returns_empty_when_professor_has_no_laboratory() {
        given(laboratoryRepository.findByProfessorId(1L)).willReturn(Optional.empty());

        Optional<LaboratoryResponseDto> result = laboratoryService.findLabByProfessorId(1L);

        assertThat(result).isEmpty();
    }

    private Laboratory laboratory() {
        Professor professor = Professor.create(
                "홍길동", "교수",
                Department.COMPUTER_ENGINEERING, "032-000-0000", "hong@inu.ac.kr"
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
}
