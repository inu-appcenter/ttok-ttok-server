package com.inuappcenter.team_2_project_server.domain.ai.dto.response;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;

import java.util.List;

/**
 * 연구실 추천 응답
 * - noMatch: 질문에 맞는 연구실이 없다고 판단한 경우 true (recommendations는 빈 목록)
 * - recommendations: 잘 맞는 순서대로 최대 3개. 확실한 것만 고르므로 3개보다 적을 수 있다
 */
public record LabRecommendationResponseDto(
        boolean noMatch,
        List<Item> recommendations
) {

    /**
     * @param reason 추천 이유. AI 선택 단계가 실패해 검색 순위로 대체한 경우 null
     */
    public record Item(
            Long laboratoryId,
            String labName,
            String department,
            String professorName,
            String reason
    ) {
        public static Item of(Laboratory laboratory, String reason) {
            return new Item(
                    laboratory.getId(),
                    laboratory.getLabName(),
                    laboratory.getDepartment().getDepartmentName(),
                    laboratory.getProfessor() == null ? null : laboratory.getProfessor().getName(),
                    reason
            );
        }
    }
}
