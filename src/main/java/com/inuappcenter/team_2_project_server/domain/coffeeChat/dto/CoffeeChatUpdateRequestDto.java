package com.inuappcenter.team_2_project_server.domain.coffeeChat.dto;

import com.inuappcenter.team_2_project_server.domain.laboratory.enums.ContactType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoffeeChatUpdateRequestDto(
        @NotNull ContactType contactType,
        @NotBlank String contactValue
) {
    @AssertTrue(message = "contactType에 맞는 형식의 연락처를 입력해주세요.")
    public boolean isContactValid() {
        if (contactType == null || contactValue == null) {
            return true; // @NotNull/@NotBlank가 따로 처리
        }
        return switch (contactType) {
            case EMAIL -> contactValue.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            case KAKAO_TALK -> contactValue.startsWith("https://open.kakao.com/");
        };
    }
}
