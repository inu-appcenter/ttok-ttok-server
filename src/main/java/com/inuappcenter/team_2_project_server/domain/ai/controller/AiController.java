package com.inuappcenter.team_2_project_server.domain.ai.controller;

import com.inuappcenter.team_2_project_server.domain.ai.dto.AiResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.LabRecommendationRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabRecommendationResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.service.chatbot.EmailEditorService;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.LabRecommendationService;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.LabSearchIndexService;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiController implements AiApiSpecification {

    private final LabRecommendationService labRecommendationService;
    private final LabSearchIndexService labSearchIndexService;
    private final EmailEditorService emailEditorService;

    /**
     * 연구실 추천 컨트롤러 (RAG)
     */
    @Override
    @PostMapping("/research-lab")
    public ResponseEntity<ResponseDto<LabRecommendationResponseDto>> researchLabChat(
            @Valid @RequestBody LabRecommendationRequestDto request
    ) {
        LabRecommendationResponseDto response = labRecommendationService.recommend(request.message());

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 추천 성공")
        );
    }

    /**
     * 연구실 추천 검색 문서 수동 동기화 컨트롤러 (관리자 전용, 매일 새벽 배치와 별개로 지금 바로 실행)
     */
    @Override
    @PostMapping("/research-lab/index/sync")
    public ResponseEntity<ResponseDto<LabSearchIndexSyncResponseDto>> syncResearchLabIndex() {
        LabSearchIndexSyncResponseDto response = labSearchIndexService.syncAll();

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 추천 검색 문서 동기화 완료")
        );
    }

    /**
     * 이메일 교정 컨트롤러 (챗봇)
     */
    @Override
    @PostMapping("/email-editor")
    public ChatResult emailEditorChat(@RequestBody ChatRequest request) {
        AiResponseDto response = emailEditorService.ask(request.message());

        String answer = response.choices()
                .get(0)
                .message()
                .content();

        return new ChatResult(answer, response.credits());
    }


    public record ChatRequest(String message) {
    }

    public record ChatResult(
            String answer,
            Double credits
    ) {
    }
}
