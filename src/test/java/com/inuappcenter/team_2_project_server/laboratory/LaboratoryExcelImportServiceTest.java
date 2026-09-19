package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LaboratoryCapacityDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.LaboratoryExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ProfessorExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.PublicationExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.LaboratoryResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Publication;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryResearchKeywordRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchAreaCategoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchKeywordRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ExcelLabParser;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.LaboratoryExcelImportService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class LaboratoryExcelImportServiceTest {

    private ExcelLabParser excelLabParser;
    private ProfessorRepository professorRepository;
    private LaboratoryRepository laboratoryRepository;
    private ResearchKeywordRepository researchKeywordRepository;
    private LaboratoryResearchKeywordRepository laboratoryResearchKeywordRepository;
    private LaboratoryExcelImportService laboratoryExcelImportService;
    private PublicationRepository publicationRepository;
    private ResearchAreaCategoryRepository researchAreaCategoryRepository;

    @BeforeEach
    void setUp() {
        excelLabParser = mock(ExcelLabParser.class);
        professorRepository = mock(ProfessorRepository.class);
        laboratoryRepository = mock(LaboratoryRepository.class);
        researchKeywordRepository = mock(ResearchKeywordRepository.class);
        laboratoryResearchKeywordRepository = mock(LaboratoryResearchKeywordRepository.class);
        publicationRepository = mock(PublicationRepository.class);
        researchAreaCategoryRepository = mock(ResearchAreaCategoryRepository.class);
        laboratoryExcelImportService = new LaboratoryExcelImportService(
                excelLabParser,
                professorRepository,
                laboratoryRepository,
                researchKeywordRepository,
                laboratoryResearchKeywordRepository,
                publicationRepository,
                researchAreaCategoryRepository
        );
    }

    @Test
    void importExcel_saves_professor_laboratory_and_research_keyword() {
        MockMultipartFile file = excelFile();
        ProfessorExcelRow professorRow = professorRow("인공지능");
        LaboratoryExcelRow laboratoryRow = laboratoryRow();
        Professor professor = professor();
        ResearchArea researchArea = ResearchArea.create("인공지능");

        given(excelLabParser.parseProfessors(file)).willReturn(List.of(professorRow));
        given(excelLabParser.parseLaboratories(file)).willReturn(List.of(laboratoryRow));
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING,
                "홍길동",
                "hong@inu.ac.kr"
        )).willReturn(Optional.empty())
                .willReturn(Optional.of(professor));
        given(professorRepository.save(any(Professor.class))).willReturn(professor);
        given(laboratoryRepository.existsByLabNameAndProfessorAndDepartment(
                "AI연구실", professor, Department.COMPUTER_ENGINEERING
        )).willReturn(false);
        given(researchKeywordRepository.findByArea("인공지능")).willReturn(Optional.empty());
        given(researchKeywordRepository.save(any(ResearchArea.class))).willReturn(researchArea);
        given(laboratoryResearchKeywordRepository.existsByLaboratoryAndResearchKeyword(
                any(Laboratory.class),
                any(ResearchArea.class)
        )).willReturn(false);

        laboratoryExcelImportService.importExcel(file);

        verify(professorRepository).save(any(Professor.class));
        verify(laboratoryRepository).save(any(Laboratory.class));
        verify(researchKeywordRepository).save(any(ResearchArea.class));
        verify(laboratoryResearchKeywordRepository).save(any(LaboratoryResearchArea.class));
    }

    @Test
    void importExcel_skips_laboratory_when_laboratory_already_exists() {
        MockMultipartFile file = excelFile();
        ProfessorExcelRow professorRow = professorRow("인공지능");
        LaboratoryExcelRow laboratoryRow = laboratoryRow();
        Professor professor = professor();

        given(excelLabParser.parseProfessors(file)).willReturn(List.of(professorRow));
        given(excelLabParser.parseLaboratories(file)).willReturn(List.of(laboratoryRow));
        given(professorRepository.findByDepartmentAndNameAndEmail(
                Department.COMPUTER_ENGINEERING,
                "홍길동",
                "hong@inu.ac.kr"
        )).willReturn(Optional.of(professor));
        given(laboratoryRepository.existsByLabNameAndProfessorAndDepartment(
                "AI연구실", professor, Department.COMPUTER_ENGINEERING
        )).willReturn(true);

        laboratoryExcelImportService.importExcel(file);

        verify(professorRepository, never()).save(any(Professor.class));
        verify(laboratoryRepository, never()).save(any(Laboratory.class));
        verify(researchKeywordRepository, never()).findByArea(anyString());
        verify(laboratoryResearchKeywordRepository, never()).save(any(LaboratoryResearchArea.class));
    }

    @Test
    void importExcel_saves_publication_when_not_duplicate() {
        MockMultipartFile file = excelFile();
        PublicationExcelRow publicationRow = publicationRow();
        Professor professor = professor();
        Laboratory laboratory = laboratory(professor);

        given(excelLabParser.parsePublications(file)).willReturn(List.of(publicationRow));
        given(laboratoryRepository.findByLabNameAndDepartmentAndProfessor_Name(
                "AI연구실", Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of(laboratory));
        given(publicationRepository.existsByLaboratoryAndTitleAndYear(
                laboratory,
                "논문 제목",
                "2024"
        )).willReturn(false);

        laboratoryExcelImportService.importExcel(file);

        verify(publicationRepository).save(any(Publication.class));
    }

    @Test
    void importExcel_skips_publication_when_already_exists() {
        MockMultipartFile file = excelFile();
        PublicationExcelRow publicationRow = publicationRow();
        Professor professor = professor();
        Laboratory laboratory = laboratory(professor);

        given(excelLabParser.parsePublications(file)).willReturn(List.of(publicationRow));
        given(laboratoryRepository.findByLabNameAndDepartmentAndProfessor_Name(
                "AI연구실", Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of(laboratory));
        given(publicationRepository.existsByLaboratoryAndTitleAndYear(
                laboratory,
                "논문 제목",
                "2024"
        )).willReturn(true);

        laboratoryExcelImportService.importExcel(file);

        verify(publicationRepository, never()).save(any(Publication.class));
    }

    @Test
    void importExcel_skips_publication_when_laboratory_name_blank() {
        MockMultipartFile file = excelFile();
        PublicationExcelRow publicationRow = new PublicationExcelRow(
                null,
                Department.COMPUTER_ENGINEERING,
                null,
                "홍길동",
                "논문 제목",
                "홍길동",
                "IEEE",
                "2024",
                "학술지",
                "게재",
                "10.1000/example",
                "https://doi.org/10.1000/example"
        );

        given(excelLabParser.parsePublications(file)).willReturn(List.of(publicationRow));

        laboratoryExcelImportService.importExcel(file);

        verify(laboratoryRepository, never()).findByLabNameAndDepartmentAndProfessor_Name(
                anyString(), any(Department.class), anyString()
        );
        verify(publicationRepository, never()).save(any(Publication.class));
    }

    @Test
    void importExcel_throws_when_laboratory_not_found_for_publication() {
        MockMultipartFile file = excelFile();
        PublicationExcelRow publicationRow = publicationRow();

        given(excelLabParser.parsePublications(file)).willReturn(List.of(publicationRow));
        given(laboratoryRepository.findByLabNameAndDepartmentAndProfessor_Name(
                "AI연구실", Department.COMPUTER_ENGINEERING, "홍길동"
        )).willReturn(List.of());

        assertThatThrownBy(() -> laboratoryExcelImportService.importExcel(file))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABORATORY_NOT_FOUND);

        verify(publicationRepository, never()).save(any(Publication.class));
    }

    private MockMultipartFile excelFile() {
        return new MockMultipartFile(
                "file",
                "laboratories.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1}
        );
    }

    private ProfessorExcelRow professorRow(String researchAreaRaw) {
        return new ProfessorExcelRow(
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING,
                "홍길동",
                "교수",
                researchAreaRaw,
                "032-000-0000",
                "hong@inu.ac.kr"
        );
    }

    private LaboratoryExcelRow laboratoryRow() {
        return new LaboratoryExcelRow(
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING,
                "AI연구실",
                "홍길동",
                "hong@inu.ac.kr",
                "7호관 401호",
                "https://lab.example.com",
                new LaboratoryCapacityDto(6, 7),
                "인공지능을 연구합니다."
        );
    }

    private PublicationExcelRow publicationRow() {
        return new PublicationExcelRow(
                "1",
                Department.COMPUTER_ENGINEERING,
                "AI연구실",
                "홍길동",
                "논문 제목",
                "홍길동, 이순신",
                "IEEE",
                "2024",
                "학술지",
                "게재",
                "10.1000/example",
                "https://doi.org/10.1000/example"
        );
    }

    private Laboratory laboratory(Professor professor) {
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

    private Professor professor() {
        return Professor.create(
                "홍길동",
                "교수",
                College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
                Department.COMPUTER_ENGINEERING,
                "032-000-0000",
                "hong@inu.ac.kr"
        );
    }
}
