package com.inuappcenter.team_2_project_server.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    PROFESSOR("교수"),
    RESEARCHER("연구생"),
    FINDER("탐색자");

    private final String description;
}
