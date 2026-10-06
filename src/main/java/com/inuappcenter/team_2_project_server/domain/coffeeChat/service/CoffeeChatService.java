package com.inuappcenter.team_2_project_server.domain.coffeeChat.service;

import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatCreateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatResponseDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.dto.CoffeeChatUpdateRequestDto;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.entity.CoffeeChat;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.repository.CoffeeChatRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CoffeeChatService {

    private final CoffeeChatRepository coffeeChatRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ResearcherRepository researcherRepository;

    /**
     * 커피챗 연구실별 조회 메서드
     */
    @Transactional(readOnly = true)
    public List<CoffeeChatResponseDto> getAllLabCoffeeChat(Long laboratoryId) {
        return coffeeChatRepository.findAllByLaboratoryId(laboratoryId)
                .stream()
                .map(CoffeeChatResponseDto::from)
                .toList();
    }

    /**
     * 커피챗 개인용 조회 메서드
     */
    @Transactional(readOnly = true)
    public CoffeeChatResponseDto getMyCoffeeChat(Long memberId) {
        return coffeeChatRepository.findByResearcherMemberId(memberId)
                .map(CoffeeChatResponseDto::from)
                .orElse(null);
    }

    /**
     * 커피챗 생성 메서드
     */
    public CoffeeChatResponseDto createCoffeeChat(Long memberId, CoffeeChatCreateRequestDto request) {
        Laboratory laboratory = laboratoryRepository.findById(request.laboratoryId())
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        Researcher researcher = researcherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MyException(ErrorCode.RESEARCHER_NOT_FOUND));

        if (coffeeChatRepository.existsByResearcherMemberId(memberId)) {
            throw new MyException(ErrorCode.COFFEE_CHAT_ALREADY_EXISTS);
        }

        CoffeeChat coffeeChat = CoffeeChat.create(
                laboratory,
                researcher,
                request.contactType(),
                request.contactValue()
        );

        // 더블클릭으로 인한 유니크 제약 조건 발생 방지 로직
        try {
            coffeeChatRepository.save(coffeeChat);
        } catch (DataIntegrityViolationException e) {
            throw new MyException(ErrorCode.COFFEE_CHAT_ALREADY_EXISTS);
        }

        return CoffeeChatResponseDto.from(coffeeChat);
    }

    /**
     * 커피챗 수정 메서드
     */
    public CoffeeChatResponseDto updateCoffeeChat(Long coffeeChatId, Long memberId, CoffeeChatUpdateRequestDto request) {
        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeeChatId)
                .orElseThrow(() -> new MyException(ErrorCode.COFFEE_CHAT_NOT_FOUND));

        validateOwner(coffeeChat, memberId);

        coffeeChat.update(
                request.contactType(),
                request.contactValue()
        );

        return CoffeeChatResponseDto.from(coffeeChat);
    }

    /**
     * 커피챗 삭제 메서드
     */
    public void deleteCoffeeChat(Long coffeeChatId, Long memberId) {
        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeeChatId)
                .orElseThrow(() -> new MyException(ErrorCode.COFFEE_CHAT_NOT_FOUND));

        validateOwner(coffeeChat, memberId);

        coffeeChatRepository.delete(coffeeChat);
    }

    private void validateOwner(CoffeeChat coffeeChat, Long memberId) {
        if (!coffeeChat.isOwnedBy(memberId)) {
            throw new MyException(ErrorCode.ACCESS_DENIED);
        }
    }
}
