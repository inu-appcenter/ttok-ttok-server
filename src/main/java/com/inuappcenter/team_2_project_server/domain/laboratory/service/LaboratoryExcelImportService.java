package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.LaboratoryExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ProfessorExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.PublicationExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ResearchAreaCategoryExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.LaboratoryResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Publication;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchArea;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.ResearchAreaCategory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryResearchKeywordRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchAreaCategoryRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchKeywordRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파싱한 엑셀 파일의 연구실을 DB에 저장하는 클래스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LaboratoryExcelImportService {

    private final ExcelLabParser excelLabParser;
    private final ProfessorRepository professorRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ResearchKeywordRepository researchKeywordRepository;
    private final LaboratoryResearchKeywordRepository laboratoryResearchKeywordRepository;
    private final PublicationRepository publicationRepository;
    private final ResearchAreaCategoryRepository researchAreaCategoryRepository;

    public void importExcel(MultipartFile file) {
        validateExcelFile(file);

        // 파싱 클래스에서 파싱해 ProfessorExcelRow과 LaboratoryExcelRow로 만든 행들을 rows에 저장
        List<ProfessorExcelRow> professorRows = excelLabParser.parseProfessors(file);
        List<LaboratoryExcelRow> laboratoryRows = excelLabParser.parseLaboratories(file);
        List<PublicationExcelRow> publicationExcelRows = excelLabParser.parsePublications(file);
        List<ResearchAreaCategoryExcelRow> researchAreaCategoryExcelRows = excelLabParser.parseResearchAreaCategories(file);

        saveProfessors(professorRows);
        saveLaboratories(laboratoryRows, professorRows);
        savePublications(publicationExcelRows);
        saveResearchAreaCategory(researchAreaCategoryExcelRows);

    }


    /**
     * 연구실 파싱 후 저장
     */
    private void saveLaboratories(
            List<LaboratoryExcelRow> laboratoryRows,
            List<ProfessorExcelRow> professorRows
    ) {
        Map<String, ProfessorExcelRow> professorInfoMap = professorRows.stream()
                .collect(Collectors.toMap(
                        row -> professorKey(row.department(), row.name(), row.email()),
                        row -> row,
                        (first, second) -> {
                            throw new MyException(ErrorCode.DUPLICATED_PROFESSOR_IN_EXCEL);
                        }
                ));

        for (LaboratoryExcelRow row : laboratoryRows) {
            ProfessorExcelRow professorInfo = professorInfoMap.get(
                    professorKey(row.department(), row.professorName(), row.professorEmail())
            );

            if (professorInfo == null) {
                throw new MyException(ErrorCode.PROFESSOR_NOT_FOUND_IN_EXCEL);
            }

            Professor professor = professorRepository.findByDepartmentAndNameAndEmail(
                            row.department(),
                            row.professorName(),
                            row.professorEmail()
                    )
                    .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));


            String researchFieldRaw = professorInfo.researchAreaRaw();

            // 연구실 중복 검증
            if (laboratoryRepository.existsByLabNameAndProfessorAndDepartment(row.labName(), professor, row.department())) {
                continue;
            }

            Laboratory laboratory = Laboratory.create(
                    row.college(),
                    row.department(),
                    row.labName(),
                    row.location(),
                    row.capacity().graduateStudentCount(),
                    row.capacity().undergraduateStudentCount(),
                    row.introduction(),
                    professor,
                    row.labUrl(),
                    researchFieldRaw
            );

            laboratoryRepository.save(laboratory);
            saveResearchKeywords(laboratory, researchFieldRaw);

        }
    }


    /**
     * 교수 파싱 후 저장
     */
    private void saveProfessors(List<ProfessorExcelRow> professorExcelRows) {
        for (ProfessorExcelRow row : professorExcelRows) {
            professorRepository.findByDepartmentAndNameAndEmail(row.department(), row.name(), row.email())
                    .orElseGet(() -> professorRepository.save(
                            Professor.create(
                                    row.name(),
                                    row.position(),
                                    row.college(),
                                    row.department(),
                                    row.number(),
                                    row.email()
                            )
                    ));
        }

    }


    /**
     * 연구 분야 키워드 저장
     */
    private void saveResearchKeywords(Laboratory laboratory, String researchFieldRaw) {
        List<String> keywordNames = splitResearchKeywords(researchFieldRaw);

        for (String keywordName : keywordNames) {
            ResearchArea keyword = researchKeywordRepository.findByArea(keywordName)
                    .orElseGet(() -> researchKeywordRepository.save(
                            ResearchArea.create(keywordName)
                    ));

            if (!laboratoryResearchKeywordRepository.existsByLaboratoryAndResearchKeyword(laboratory, keyword)) {
                laboratoryResearchKeywordRepository.save(
                        LaboratoryResearchArea.create(laboratory, keyword)
                );
            }
        }
    }

    // 엑셀 원문 데이터를 쪼개는 메서드
    private List<String> splitResearchKeywords(String researchFieldRaw) {
        if (researchFieldRaw == null || researchFieldRaw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(researchFieldRaw.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .toList();
    }

    // 엑셀 파일 검증 메서드
    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MyException(ErrorCode.EMPTY_EXCEL_FILE);
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.endsWith(".xlsx")) {
            throw new MyException(ErrorCode.NOT_FILE_EXTENDER);
        }
    }

    private String professorKey(Department department, String professorName, String professorEmail) {
        return department.name() + "|" + professorName + "|" + professorEmail;
    }

    /**
     * 논문 저장 메서드
     */
    private void savePublications(List<PublicationExcelRow> rows) {
        for (PublicationExcelRow row : rows) {
            // 연구실이 존재하지 않는 논문은 스킵
            if (row.laboratoryName() == null || row.laboratoryName().isBlank()) {
                continue;
            }

            // 연구실이 있는 논문만 아래 로직을 탐
            List<Laboratory> laboratories = laboratoryRepository.findByLabNameAndDepartmentAndProfessor_Name(row.laboratoryName(), row.department(), row.professorName());

            if (laboratories.isEmpty()) {
                throw new MyException(ErrorCode.LABORATORY_NOT_FOUND);
            }

            Laboratory laboratory = laboratories.getFirst();
            Professor professor = laboratory.getProfessor();

            if (publicationRepository.existsByLaboratoryAndTitleAndYear(
                    laboratory, row.title(), row.year())) {
                continue;
            }

            Publication publication = Publication.create(
                    laboratory,
                    professor,
                    row.title(),
                    row.researchersRaw(),
                    row.platform(),
                    row.year(),
                    row.type(),
                    row.status(),
                    row.doi(),
                    row.sourceURL()
            );

            publicationRepository.save(publication);
        }
    }

    /**
     * 연구 분야 카테고리 저장 메서드
     */
    private void saveResearchAreaCategory(List<ResearchAreaCategoryExcelRow> rows) {
        for (ResearchAreaCategoryExcelRow row : rows) {
            if (row.categoryName() == null || row.areaName() == null) {
                continue;
            }

            ResearchAreaCategory category = researchAreaCategoryRepository.findByCategoryName(row.categoryName())
                    .orElseGet(() -> researchAreaCategoryRepository.save(
                            ResearchAreaCategory.create(row.categoryName())
                    ));

            ResearchArea area = researchKeywordRepository.findByArea(row.areaName())
                    .orElseThrow(() -> new MyException(ErrorCode.RESEARCH_KEYWORD_NOT_FOUND));

            area.updateCategory(category);
        }
    }
}
