package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.LaboratoryExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ProfessorExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.service.ExcelLabParser;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelLabParserTest {

    private final ExcelLabParser excelLabParser = new ExcelLabParser();

    @Test
    void 엑셀파일_연구실_시트_파싱_테스트() throws IOException {
        MockMultipartFile file = workbookFile(createValidWorkbook());

        List<LaboratoryExcelRow> rows = excelLabParser.parseLaboratories(file);

        assertThat(rows).hasSize(1);
        LaboratoryExcelRow row = rows.getFirst();
        assertThat(row.college()).isEqualTo(College.COLLEGE_OF_INFORMATION_TECHNOLOGY);
        assertThat(row.department()).isEqualTo(Department.COMPUTER_ENGINEERING);
        assertThat(row.labName()).isEqualTo("AI연구실");
        assertThat(row.professorName()).isEqualTo("홍길동");
        assertThat(row.professorEmail()).isEqualTo("hong@inu.ac.kr");
        assertThat(row.location()).isEqualTo("7호관 401호");
        assertThat(row.labUrl()).isEqualTo("https://lab.example.com");
        assertThat(row.capacity().graduateStudentCount()).isEqualTo(6);
        assertThat(row.capacity().undergraduateStudentCount()).isEqualTo(7);
        assertThat(row.introduction()).isEqualTo("인공지능을 연구합니다.");
    }

    @Test
    void 엑셀파일_교수_시트_파싱_테스트() throws IOException {
        MockMultipartFile file = workbookFile(createValidWorkbook());

        List<ProfessorExcelRow> rows = excelLabParser.parseProfessors(file);

        assertThat(rows).hasSize(1);
        ProfessorExcelRow row = rows.getFirst();
        assertThat(row.college()).isEqualTo(College.COLLEGE_OF_INFORMATION_TECHNOLOGY);
        assertThat(row.department()).isEqualTo(Department.COMPUTER_ENGINEERING);
        assertThat(row.name()).isEqualTo("홍길동");
        assertThat(row.position()).isEqualTo("교수");
        assertThat(row.researchAreaRaw()).isEqualTo("인공지능");
        assertThat(row.number()).isEqualTo("032-000-0000");
        assertThat(row.email()).isEqualTo("hong@inu.ac.kr");
    }

    @Test
    void 엑셀파일_헤더_검증_테스트() throws IOException {
        Workbook workbook = createValidWorkbook();
        workbook.getSheetAt(0).getRow(0).getCell(5).setCellValue("이메일");
        MockMultipartFile file = workbookFile(workbook);

        assertThatThrownBy(() -> excelLabParser.parseLaboratories(file))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EXCEL_HEADER);
    }

    private Workbook createValidWorkbook() {
        Workbook workbook = new XSSFWorkbook();
        createLaboratorySheet(workbook);
        createProfessorSheet(workbook);
        createPublicationSheet(workbook);
        createResearchAreaCategorySheet(workbook);
        return workbook;
    }

    private void createLaboratorySheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("연구실");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("번호");
        header.createCell(1).setCellValue("단과대");
        header.createCell(2).setCellValue("학과/전공");
        header.createCell(3).setCellValue("연구실명");
        header.createCell(4).setCellValue("지도교수");
        header.createCell(5).setCellValue("교수 이메일");
        header.createCell(6).setCellValue("연구실 위치");
        header.createCell(7).setCellValue("개별 연구실 URL");
        header.createCell(8).setCellValue("인원수");
        header.createCell(9).setCellValue("연구실 요약");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("1");
        row.createCell(1).setCellValue("정보기술대학");
        row.createCell(2).setCellValue("컴퓨터공학부");
        row.createCell(3).setCellValue("AI연구실");
        row.createCell(4).setCellValue("홍길동");
        row.createCell(5).setCellValue("hong@inu.ac.kr");
        row.createCell(6).setCellValue("7호관 401호");
        row.createCell(7).setCellValue("https://lab.example.com");
        row.createCell(8).setCellValue("석박사: 6명, 학부: 7명");
        row.createCell(9).setCellValue("인공지능을 연구합니다.");
    }

    private void createResearchAreaCategorySheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("연구카테고리");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("카테고리");
        header.createCell(1).setCellValue("연구분야 ID");
        header.createCell(2).setCellValue("연구분야명");
        header.createCell(3).setCellValue("연구실 ID");
        header.createCell(4).setCellValue("연구실명");
        header.createCell(5).setCellValue("지도교수");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("AI");
        row.createCell(1).setCellValue("1");
        row.createCell(2).setCellValue("인공지능");
        row.createCell(3).setCellValue("1");
        row.createCell(4).setCellValue("AI연구실");
        row.createCell(5).setCellValue("홍길동");
    }

    private void createProfessorSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("교수정보");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("번호");
        header.createCell(1).setCellValue("단과대");
        header.createCell(2).setCellValue("학과/전공");
        header.createCell(3).setCellValue("이름");
        header.createCell(4).setCellValue("직책/직급");
        header.createCell(5).setCellValue("연구분야/주전공");
        header.createCell(6).setCellValue("전화번호");
        header.createCell(7).setCellValue("이메일");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("1");
        row.createCell(1).setCellValue("정보기술대학");
        row.createCell(2).setCellValue("컴퓨터공학부");
        row.createCell(3).setCellValue("홍길동");
        row.createCell(4).setCellValue("교수");
        row.createCell(5).setCellValue("인공지능");
        row.createCell(6).setCellValue("032-000-0000");
        row.createCell(7).setCellValue("hong@inu.ac.kr");
    }

    private void createPublicationSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("연구실논문");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("번호");
        header.createCell(1).setCellValue("연구실번호");
        header.createCell(2).setCellValue("학과/전공");
        header.createCell(3).setCellValue("연구실명");
        header.createCell(4).setCellValue("지도교수");
        header.createCell(5).setCellValue("논문명");
        header.createCell(6).setCellValue("저자");
        header.createCell(7).setCellValue("게재처");
        header.createCell(8).setCellValue("연도");
        header.createCell(9).setCellValue("논문유형");
        header.createCell(10).setCellValue("상태");
        header.createCell(11).setCellValue("DOI");
        header.createCell(12).setCellValue("논문URL");
        header.createCell(13).setCellValue("출처유형");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("1");
        row.createCell(1).setCellValue("1");
        row.createCell(2).setCellValue("컴퓨터공학부");
        row.createCell(3).setCellValue("AI연구실");
        row.createCell(4).setCellValue("홍길동");
        row.createCell(5).setCellValue("논문 제목");
        row.createCell(6).setCellValue("홍길동, 이순신");
        row.createCell(7).setCellValue("IEEE");
        row.createCell(8).setCellValue("2024");
        row.createCell(9).setCellValue("학술지");
        row.createCell(10).setCellValue("게재");
        row.createCell(11).setCellValue("10.1000/example");
        row.createCell(12).setCellValue("https://doi.org/10.1000/example");
        row.createCell(13).setCellValue("국내");
    }

    private MockMultipartFile workbookFile(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile(
                "file",
                "laboratories.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );
    }
}
