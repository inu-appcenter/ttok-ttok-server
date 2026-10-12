package com.inuappcenter.team_2_project_server.onboarding;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.domain.onboarding.dto.OnboardingRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void professor_does_not_need_student_department() {
        // 교수는 department 대신 professorDepartment로 학과를 보낸다
        assertThat(validator.validate(new OnboardingRequestDto(
                UserType.PROFESSOR, Department.CHEMISTRY, "홍교수",
                null, null, null, null, null, null, null, null
        ))).isEmpty();
    }

    @Test
    void finder_with_department_is_valid() {
        assertThat(validator.validate(finder(Department.KOREAN))).isEmpty();
    }

    @Test
    void student_department_is_required() {
        assertThat(invalidFields(finder(null))).containsExactly("studentDepartmentPresent");
    }

    @Test
    void researcher_fields_are_required() {
        assertThat(invalidFields(new OnboardingRequestDto(
                UserType.RESEARCHER, null, null,
                null, null, null, null, false, null, null,
                Department.COMPUTER_ENGINEERING
        ))).containsExactly("researcherFieldsPresent");
    }

    @Test
    void professor_name_is_required_for_professor() {
        assertThat(invalidFields(new OnboardingRequestDto(
                UserType.PROFESSOR, Department.CHEMISTRY, " ",
                null, null, null, null, null, null, null, null
        ))).containsExactly("professorNamePresent");
    }

    private Set<String> invalidFields(OnboardingRequestDto request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private OnboardingRequestDto finder(Department department) {
        return new OnboardingRequestDto(
                UserType.FINDER, null, null,
                null, null, null, null, false, null, null,
                department
        );
    }
}
