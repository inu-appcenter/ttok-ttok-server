package com.inuappcenter.team_2_project_server.domain.coffeeChat.dto;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.entity.CoffeeChat;
import com.inuappcenter.team_2_project_server.domain.laboratory.enums.ContactType;

public record CoffeeChatResponseDto(
        Long id,
        Long laboratoryId,
        String laboratoryName,
        Long researcherId,
        ContactType contactType,
        String contactValue
) {
    public static CoffeeChatResponseDto from(CoffeeChat coffeeChat) {
        return new CoffeeChatResponseDto(
                coffeeChat.getId(),
                coffeeChat.getLaboratory().getId(),
                coffeeChat.getLaboratory().getLabName(),
                coffeeChat.getResearcher().getId(),
                coffeeChat.getContactType(),
                coffeeChat.getContactValue()
        );
    }
}
