package com.inuappcenter.team_2_project_server.domain.department.controller;

import com.inuappcenter.team_2_project_server.domain.department.dto.response.CollegeResponseDto;
import com.inuappcenter.team_2_project_server.domain.department.dto.response.DepartmentResponseDto;
import com.inuappcenter.team_2_project_server.domain.department.service.DepartmentService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/college")
public class DepartmentController implements DepartmentApiSpecification {

    private final DepartmentService departmentService;

    /**
     * 단과대 전체 조회 컨트롤러
     */
    @Override
    @GetMapping
    public ResponseEntity<ResponseDto<List<CollegeResponseDto>>> getAllColleges() {
        List<CollegeResponseDto> responses = departmentService.getAllColleges();

        return ResponseEntity.ok(
                ResponseDto.of(responses, "단과대 전체 조회 성공")
        );
    }

    /**
     * 학과 전체 조회 컨트롤러
     */
    @Override
    @GetMapping("/department")
    public ResponseEntity<ResponseDto<List<DepartmentResponseDto>>> getAllDepartments() {
        List<DepartmentResponseDto> responses = departmentService.getAllDepartments();

        return ResponseEntity.ok(
                ResponseDto.of(responses, "학과 전체 조회 성공")
        );
    }
}
