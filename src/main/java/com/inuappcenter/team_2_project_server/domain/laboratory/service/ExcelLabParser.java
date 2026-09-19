package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.LaboratoryCapacityDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.LaboratoryExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ProfessorExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.PublicationExcelRow;
import com.inuappcenter.team_2_project_server.domain.laboratory.dto.parse.ResearchAreaCategoryExcelRow;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파싱만 담담하는 클래스
 */
@Component
public class ExcelLabParser {

    /**
     * 실제 엑셀 파일 연구실쪽 파싱 로직(시트 1번)
     */
    public List<LaboratoryExcelRow> parseLaboratories(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            validateWorkbook(workbook);

            // 엑셀이 첫번째 시트를 가져옴
            Sheet sheet = workbook.getSheetAt(0);

            // 엑셀 셀 값을 문자열로 안전하게 읽기 위한 도구. 셀이 숫자든 문자든 날짜든 상관없이 문자열로 바꿔줌.
            DataFormatter formatter = new DataFormatter(Locale.KOREA);

            // 파싱된 행들을 담을 리스트 생성
            List<LaboratoryExcelRow> rows = new ArrayList<>();

            // 1번째 행부터 반복 파싱(0번째 행은 보통 헤더)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row, formatter, 10)) {
                    continue;
                }

                rows.add(toLaboratoryExcelRow(row, formatter));
            }
            return rows;
        } catch (MyException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new MyException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    // 파싱 로직에서 가져온 엑셀 행을 LaboratoryExcelRow로 변환
    private LaboratoryExcelRow toLaboratoryExcelRow(Row row, DataFormatter formatter) {
        College college = College.fromCollegeName(getString(row, 1, formatter));
        Department department = Department.fromDepartmentName(getString(row, 2, formatter));
        String capacityRaw = getString(row, 8, formatter);

        validateCollegeAndDepartment(college, department);

        return new LaboratoryExcelRow(
                college,
                department,
                getString(row, 3, formatter), // 연구실명
                getString(row, 4, formatter), // 지도교수
                getString(row, 5, formatter), // 교수 이메일
                getString(row, 6, formatter), // 연구실 위치
                getString(row, 7, formatter), // 개별 연구실 URL
                parseCapacity(capacityRaw),  // 인원수
                getString(row, 9, formatter)
        );
    }

    /**
     * 실제 엑셀 파일 교수쪽 파싱 로직(시트 2번)
     */
    public List<ProfessorExcelRow> parseProfessors(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            validateWorkbook(workbook);

            // 엑셀 파일의 첫 번째 시트를 가져옴
            Sheet sheet = workbook.getSheetAt(1);

            // DataFormatter로 한글
            DataFormatter formatter = new DataFormatter(Locale.KOREA);

            // 엑셀에서 가져온 행을 저장할 객체
            List<ProfessorExcelRow> rows = new ArrayList<>();

            // 첫 번째 열부터 파싱(0번째 열은 번호)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row, formatter, 9)) {
                    continue;
                }

                rows.add(toProfessorExcelRow(row, formatter));
            }
            return rows;
        } catch (MyException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new MyException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    // 파싱 로직에서 가져온 엑셀 행을 ProfessorExcelRow로 변환
    private ProfessorExcelRow toProfessorExcelRow(Row row, DataFormatter formatter) {
        // enum값을 따로 처리
        College college = College.fromCollegeName(getString(row, 1, formatter));
        Department department = Department.fromDepartmentName(getString(row, 2, formatter));

        validateCollegeAndDepartment(college, department);

        return new ProfessorExcelRow(
                college,
                department,
                getString(row, 3, formatter),
                getString(row, 4, formatter),
                getString(row, 5, formatter),
                getString(row, 6, formatter),
                getString(row, 7, formatter)
        );
    }

    /**
     * 실제 엑셀 파일 논문쪽 파싱 로직(시트 3번)
     */
    public List<PublicationExcelRow> parsePublications(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            validateWorkbook(workbook);

            // 엑셀 파일의 첫 번째 시트를 가져옴
            Sheet sheet = workbook.getSheetAt(2);

            // DataFormatter로 한글
            DataFormatter formatter = new DataFormatter(Locale.KOREA);

            // 엑셀에서 가져온 행을 저장할 객체
            List<PublicationExcelRow> rows = new ArrayList<>();

            // 첫 번째 열부터 파싱(0번째 열은 번호)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row, formatter, 14)) {
                    continue;
                }

                rows.add(toPublicationExcelRow(row, formatter));
            }
            return rows;
        } catch (MyException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new MyException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    private PublicationExcelRow toPublicationExcelRow(Row row, DataFormatter formatter) {
        // enum값을 따로 처리
        Department department = Department.fromDepartmentName(getString(row, 2, formatter));

        return new PublicationExcelRow(
                getString(row, 1, formatter),
                department,
                getString(row, 3, formatter),
                getString(row, 4, formatter),
                getString(row, 5, formatter),
                getString(row, 6, formatter),
                getString(row, 7, formatter),
                getString(row, 8, formatter),
                getString(row, 9, formatter),
                getString(row, 10, formatter),
                getString(row, 11, formatter),
                getString(row, 12, formatter)
        );
    }

    /**
     * 실제 엑셀 파일 연구분야쪽 파싱 로직(시트 4번)
     */
    public List<ResearchAreaCategoryExcelRow> parseResearchAreaCategories(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            validateWorkbook(workbook);

            // 엑셀 파일의 첫 번째 시트를 가져옴
            Sheet sheet = workbook.getSheetAt(3);

            // DataFormatter로 한글
            DataFormatter formatter = new DataFormatter(Locale.KOREA);

            // 엑셀에서 가져온 행을 저장할 객체
            List<ResearchAreaCategoryExcelRow> rows = new ArrayList<>();

            // 첫 번째 열부터 파싱(0번째 열은 번호)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row, formatter, 6)) {
                    continue;
                }

                rows.add(toResearchAreaCategoryExcelRow(row, formatter));
            }
            return rows;
        } catch (MyException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new MyException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    private ResearchAreaCategoryExcelRow toResearchAreaCategoryExcelRow(Row row, DataFormatter formatter) {
        return new ResearchAreaCategoryExcelRow(
                getString(row, 0, formatter),
                getString(row, 2, formatter)
        );
    }


    // 학과가 해당 단과대에 있는 지 확인
    private void validateCollegeAndDepartment(College college, Department department) {
        if (department.getCollegeName() != college) {
            throw new MyException(ErrorCode.INVALID_EXCEL_ROW);
        }
    }

    // 엑셀 셀은 숫자/문자 타입이 섞일 수 있으니까 DataFormatter로 읽음
    private String getString(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);

        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell);
        return value == null || value.isBlank() ? null : value.trim();
    }

    // 비어있는 행이 있으면 패스
    private boolean isBlankRow(Row row, DataFormatter formatter, int columnCount) {
        for (int cellIndex = 0; cellIndex < columnCount; cellIndex++) {
            String value = getString(row, cellIndex, formatter);

            if (value != null) {
                return false;
            }
        }

        return true;
    }

    // 엑셀 파일 헤더 검증 메서드
    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() < 4) {
            throw new MyException(ErrorCode.INVALID_EXCEL_SHEET);
        }

        validateSheetName(workbook.getSheetAt(0), "연구실");
        validateSheetName(workbook.getSheetAt(1), "교수정보");
        validateSheetName(workbook.getSheetAt(2), "연구실논문");
        validateSheetName(workbook.getSheetAt(3), "연구카테고리");

        DataFormatter formatter = new DataFormatter(Locale.KOREA);

        validateHeader(
                workbook.getSheetAt(0),
                formatter,
                List.of("번호", "단과대", "학과/전공", "연구실명", "지도교수", "교수 이메일", "연구실 위치", "개별 연구실 URL", "인원수", "연구실 요약")
        );

        validateHeader(
                workbook.getSheetAt(1),
                formatter,
                List.of("번호", "단과대", "학과/전공", "이름", "직책/직급", "연구분야/주전공", "전화번호", "이메일")
        );

        validateHeader(
                workbook.getSheetAt(2),
                formatter,
                List.of("번호", "연구실번호", "학과/전공", "연구실명", "지도교수", "논문명", "저자", "게재처", "연도", "논문유형", "상태", "DOI", "논문URL", "출처유형")
        );

        validateHeader(
                workbook.getSheetAt(3),
                formatter,
                List.of("카테고리", "연구분야 ID", "연구분야명", "연구실 ID", "연구실명", "지도교수")
        );
    }

    // 시트 이름 검증
    private void validateSheetName(Sheet sheet, String expectedName) {
        if (!expectedName.equals(sheet.getSheetName())) {
            throw new MyException(ErrorCode.INVALID_EXCEL_SHEET);
        }
    }

    // 헤더 검증
    private void validateHeader(Sheet sheet, DataFormatter formatter, List<String> expectedHeaders) {
        Row headerRow = sheet.getRow(0);

        if (headerRow == null) {
            throw new MyException(ErrorCode.INVALID_EXCEL_HEADER);
        }

        for (int cellIndex = 0; cellIndex < expectedHeaders.size(); cellIndex++) {
            String actualHeader = getString(headerRow, cellIndex, formatter);
            String expectedHeader = expectedHeaders.get(cellIndex);

            if (!expectedHeader.equals(actualHeader)) {
                throw new MyException(ErrorCode.INVALID_EXCEL_HEADER);
            }
        }
    }

    // 인원수 문자열 파싱 로직
    private LaboratoryCapacityDto parseCapacity(String capacityRaw) {
        if (capacityRaw == null || capacityRaw.isBlank()) {
            return new LaboratoryCapacityDto(null, null);
        }

        Integer graduateStudentCount = extractCount(capacityRaw, "석박사");
        Integer undergraduateStudentCount = extractCount(capacityRaw, "학부");

        return new LaboratoryCapacityDto(
                graduateStudentCount,
                undergraduateStudentCount
        );
    }

    // 인원수 문자열에서 숫자 뽑아내는 메서드
    private Integer extractCount(String raw, String label) {
        Pattern pattern = Pattern.compile(label + "\\s*:\\s*(\\d+)\\s*명 ?");
        Matcher matcher = pattern.matcher(raw);

        if (!matcher.find()) {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
    }
}
