package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.dto.response.ResearchAreaCategoryResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.ResearchAreaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResearchAreaCategoryService {

    private final ResearchAreaCategoryRepository researchAreaCategoryRepository;

    public List<ResearchAreaCategoryResponseDto> getAllResearchAreaCategory() {
        return researchAreaCategoryRepository.findAll()
                .stream()
                .map(ResearchAreaCategoryResponseDto::from)
                .toList();
    }
}
