package com.inuappcenter.team_2_project_server.domain.onboarding.dto;

import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.enums.ContactType;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * 온보딩에서 사용자가 선택한 답변을 한 번에 받는 요청 DTO.
 * purpose 에 따라 필요한 필드가 달라지므로 필드 단위 제약 대신 @AssertTrue 로 조건부 검증한다.
 * 교수(PROFESSOR)는 로그인 시점에 이미 결정되어 이 값을 보지 않으므로, 여기서는 사실상 RESEARCHER/FINDER만 의미가 있다.
 */
public record OnboardingRequestDto(

        @NotNull
        UserType purpose,

        // purpose = PROFESSOR 일 때만 사용. 학과+이름으로 검색해 아직 연동 안 된 교수 레코드를 자동으로 연결한다.
        // 이름만으로는 동명이인이 있을 수 있어 학과까지 받는다
        Department professorDepartment,
        String professorName,

        // purpose = RESEARCHER 일 때만 사용
        Long laboratoryId,           // 소속 연구실
        String coreTime,             // "있음" / "없음"
        String weeklyMeeting,        // "주 1회" 등
        Set<String> doings,          // 주로 하는 일 (최대 3개, 프론트에서 제한)

        // 커피챗 (RESEARCHER 이면서 허용한 경우에만)
        Boolean coffeeChatAllowed,
        ContactType contactType,
        String contactValue
) {

    @AssertTrue(message = "교수 온보딩에는 본인 학과와 이름이 필요합니다.")
    public boolean isProfessorNamePresent() {
        if (purpose != UserType.PROFESSOR) {
            return true;
        }
        return professorDepartment != null && professorName != null && !professorName.isBlank();
    }

    @AssertTrue(message = "연구생 온보딩에는 연구실, 코어타임, 미팅 빈도, 하는 일이 모두 필요합니다.")
    public boolean isResearcherFieldsPresent() {
        if (purpose != UserType.RESEARCHER) {
            return true;
        }
        return laboratoryId != null
                && coreTime != null && !coreTime.isBlank()
                && weeklyMeeting != null && !weeklyMeeting.isBlank()
                && doings != null && !doings.isEmpty();
    }

    @AssertTrue(message = "커피챗을 허용하면 연락처 유형과 값이 필요합니다.")
    public boolean isCoffeeChatContactPresent() {
        if (purpose != UserType.RESEARCHER
                || !Boolean.TRUE.equals(coffeeChatAllowed)) {
            return true;
        }
        return contactType != null && contactValue != null && !contactValue.isBlank();
    }

    @AssertTrue(message = "contactType에 맞는 형식의 연락처를 입력해주세요.")
    public boolean isCoffeeChatContactFormatValid() {
        if (purpose != UserType.RESEARCHER
                || !Boolean.TRUE.equals(coffeeChatAllowed)
                || contactType == null || contactValue == null) {
            return true;
        }
        return switch (contactType) {
            case EMAIL -> contactValue.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            case KAKAO_TALK -> contactValue.startsWith("https://open.kakao.com/");
        };
    }
}
