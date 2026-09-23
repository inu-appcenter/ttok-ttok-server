package com.inuappcenter.team_2_project_server.domain.ai.controller;

import com.inuappcenter.team_2_project_server.domain.ai.dto.AiResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiController implements AiApiSpecification {

    private final AiService aiService;

    @PostMapping("/research-lab")
    public ChatResult researchLabChat(@RequestBody ChatRequest request) {
        AiResponseDto response = aiService.researchLabAsk(request.message());

        String answer = response.choices()
                .get(0)
                .message()
                .content();

        return new ChatResult(answer, response.credits());
    }

    @PostMapping("/email-editor")
    public ChatResult emailEditorChat(@RequestBody ChatRequest request) {
        AiResponseDto response = aiService.emailEditorAsk(request.message());

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
