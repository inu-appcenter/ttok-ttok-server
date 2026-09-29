package com.inuappcenter.team_2_project_server.domain.laboratory.dto;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;

/**
 * 단과대/학과별 연구실 개수 집계 쿼리의 행(row) 하나를 담는 flat DTO.
 * 최종 응답 모양(LabCountByCollegeResponseDto, 단과대 안에 학과 리스트가 중첩된 형태)과는
 * 타입이 달라서 별도로 둔다 — JPQL 생성자 표현식은 이 flat 타입으로만 매핑 가능하다
 */
public record LabCollegeDeptCountRow(
        College college,
        Department department,
        Long count
) {
}
