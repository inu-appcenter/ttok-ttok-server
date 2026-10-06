package com.inuappcenter.team_2_project_server.domain.coffeeChat.controller;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatResponseDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.service.CoffeeChatService;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coffee-chat")
public class CoffeeChatController implements CoffeeChatApiSpecification {

    private final CoffeeChatService coffeeChatService;

    /**
     * 연구실 커피챗 목록 조회 컨트롤러
     */
    @GetMapping("/laboratory")
    public ResponseEntity<ResponseDto<List<CoffeeChatResponseDto>>> getAllLabCoffeeChat(
            @RequestParam Long laboratoryId
    ) {
        List<CoffeeChatResponseDto> response = coffeeChatService.getAllLabCoffeeChat(laboratoryId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "연구실 커피챗 목록 조회 성공")
        );
    }

    /**
     * 내 커피챗 조회 컨트롤러
     */
    @GetMapping("/me")
    public ResponseEntity<ResponseDto<CoffeeChatResponseDto>> getMyCoffeeChat(
            @AuthenticationPrincipal Member member
    ) {
        CoffeeChatResponseDto response = coffeeChatService.getMyCoffeeChat(member.getId());

        return ResponseEntity.ok(
                ResponseDto.of(response, "내 커피챗 조회 성공")
        );
    }

    /**
     * 커피챗 생성 컨트롤러
     */
    @PostMapping
    public ResponseEntity<ResponseDto<CoffeeChatResponseDto>> createCoffeeChat(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody CoffeeChatCreateRequestDto request
    ) {
        CoffeeChatResponseDto response = coffeeChatService.createCoffeeChat(member.getId(), request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "커피챗 생성 성공")
        );
    }

    /**
     * 커피챗 수정 컨트롤러
     */
    @PatchMapping("/{coffeeChatId}")
    public ResponseEntity<ResponseDto<CoffeeChatResponseDto>> updateCoffeeChat(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody CoffeeChatUpdateRequestDto request,
            @PathVariable Long coffeeChatId
    ) {
        CoffeeChatResponseDto response = coffeeChatService.updateCoffeeChat(coffeeChatId, member.getId(), request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "커피챗 수정 성공")
        );
    }

    /**
     * 커피챗 삭제 컨트롤러
     */
    @DeleteMapping("/{coffeeChatId}")
    public ResponseEntity<ResponseDto<Long>> deleteCoffeeChat(
            @AuthenticationPrincipal Member member,
            @PathVariable Long coffeeChatId

    ) {
        coffeeChatService.deleteCoffeeChat(coffeeChatId, member.getId());

        return ResponseEntity.ok(
                ResponseDto.of(coffeeChatId, "커피챗 삭제 성공")
        );
    }
}
