package com.inuappcenter.team_2_project_server.domain.publication.service;

import com.inuappcenter.team_2_project_server.domain.publication.dto.PublicationResponseDto;
import com.inuappcenter.team_2_project_server.domain.publication.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicationService {
    private final LaboratoryRepository laboratoryRepository;
    private final PublicationRepository publicationRepository;

    @Transactional(readOnly = true)
    public Page<PublicationResponseDto> getLabPublications(Long laboratoryId, Pageable pageable) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        return publicationRepository.findByLaboratory(laboratory, pageable)
                .map(PublicationResponseDto::from);
    }
}
