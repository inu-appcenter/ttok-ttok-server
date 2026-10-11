package com.inuappcenter.team_2_project_server.ai;

import com.inuappcenter.team_2_project_server.domain.ai.dto.request.EmailRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.enums.EmailPurpose;
import com.inuappcenter.team_2_project_server.domain.ai.enums.Grade;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmailRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void valid_without_optional_phone_number_and_content() {
        assertThat(validator.validate(request("ttokttok@gmail.com", "수전해 촉매", null))).isEmpty();
    }

    @Test
    void interest_is_required() {
        assertThat(invalidFields(request("ttokttok@gmail.com", " ", null))).containsExactly("interest");
    }

    @Test
    void email_must_be_email_format() {
        assertThat(invalidFields(request("not-an-email", "수전해 촉매", null))).containsExactly("email");
    }

    @Test
    void content_is_at_most_1200_chars() {
        assertThat(invalidFields(request("ttokttok@gmail.com", "수전해 촉매", "가".repeat(1201)))).containsExactly("content");
    }

    private Set<String> invalidFields(EmailRequestDto request) {
        Set<ConstraintViolation<EmailRequestDto>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
    }

    private EmailRequestDto request(String email, String interest, String content) {
        return new EmailRequestDto(
                EmailPurpose.UNDERGRADUATE_RESEARCH, 1L, "홍길동", Department.COMPUTER_ENGINEERING, Grade.THIRD,
                email, null, interest, content
        );
    }
}
