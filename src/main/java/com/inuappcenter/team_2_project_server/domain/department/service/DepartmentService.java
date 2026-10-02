package com.inuappcenter.team_2_project_server.domain.department.service;

import com.inuappcenter.team_2_project_server.domain.department.dto.response.CollegeResponseDto;
import com.inuappcenter.team_2_project_server.domain.department.dto.response.DepartmentResponseDto;
import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 단과대/학과는 enum으로 관리하므로 DB 조회 없이 enum 선언 순서대로 반환
 */
@Service
public class DepartmentService {

    /**
     * 단과대 전체 조회 메서드
     */
    public List<CollegeResponseDto> getAllColleges() {
        return Arrays.stream(College.values())
                .map(CollegeResponseDto::from)
                .toList();
    }

    /**
     * 학과 전체 조회 메서드
     */
    public List<DepartmentResponseDto> getAllDepartments() {
        return Arrays.stream(Department.values())
                .map(DepartmentResponseDto::from)
                .toList();
    }
}
