package com.inuappcenter.team_2_project_server.domain.ai.dto.request;

import com.inuappcenter.team_2_project_server.domain.ai.enums.EmailPurpose;
import com.inuappcenter.team_2_project_server.domain.ai.enums.Grade;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 교수님께 메일 쓰기 요청 (연구실 상세 화면에서만 연다)
 * - purpose: 학부연구생 지원 / 대학원 진학 상담 / 교수님께 질문
 * - laboratoryId: 메일을 받을 연구실. 연구실 정보와 담당 교수 이름은 서버가 DB에서 채운다
 * - name, department, grade: 메일 자기소개와 서명에 쓴다
 * - email, phoneNumber: 화면 입력값. 메일 초안에는 넣지 않는다
 * - interest: 관심 있는 연구
 * - content: 관련해서 해본 일 (선택)
 */
public record EmailRequestDto(
        @NotNull EmailPurpose purpose,
        @NotNull Long laboratoryId,
        @NotBlank @Size(max = 20) String name,
        @NotNull Department department,
        @NotNull Grade grade,
        @NotBlank @Email String email,
        @Size(max = 13) String phoneNumber,
        @NotBlank @Size(max = 300) String interest,
        @Size(max = 1200) String content
) {
}
