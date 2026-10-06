package com.inuappcenter.team_2_project_server.domain.researchMetric.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.publication.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.researchMetric.client.OpenAlexClient;
import com.inuappcenter.team_2_project_server.domain.researchMetric.dto.response.ResearchMetricResponseDto;
import com.inuappcenter.team_2_project_server.domain.researchMetric.entity.ResearchMetric;
import com.inuappcenter.team_2_project_server.domain.researchMetric.repository.ResearchMetricRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResearchMetricService {

    // "최근 5년 논문" 기준 (올해 포함 5개년)
    private static final int RECENT_YEARS = 5;

    // OpenAlex 저자 ID 형식 (예: A5067381195)
    private static final Pattern OPEN_ALEX_AUTHOR_ID = Pattern.compile("^A\\d+$");

    private final LaboratoryRepository laboratoryRepository;
    private final ProfessorRepository professorRepository;
    private final PublicationRepository publicationRepository;
    private final ResearchMetricRepository researchMetricRepository;
    private final ResearchMetricSyncService researchMetricSyncService;

    /**
     * 연구실 연구 지표 조회 메서드 (배치가 저장해둔 값만 읽는다)
     * 담당 교수가 없는 연구실은 null 반환
     */
    public ResearchMetricResponseDto getLabMetrics(Long laboratoryId) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        Professor professor = laboratory.getProfessor();
        if (professor == null) {
            return null;
        }

        return toResponse(professor, researchMetricRepository.findByProfessorId(professor.getId()).orElse(null));
    }

    /**
     * 관리자가 교수의 OpenAlex 저자 ID를 직접 지정하고, 바로 지표를 받아오는 메서드
     * 자동 매칭이 보류됐거나 잘못 매칭된 교수를 바로잡을 때 사용 (이후 배치의 자동 매칭이 덮어쓰지 않음)
     */
    @Transactional
    public ResearchMetricResponseDto assignAuthor(Long professorId, String openAlexAuthorId) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new MyException(ErrorCode.PROFESSOR_NOT_FOUND));

        String shortAuthorId = OpenAlexClient.toShortId(openAlexAuthorId);
        if (shortAuthorId == null || !OPEN_ALEX_AUTHOR_ID.matcher(shortAuthorId).matches()) {
            throw new MyException(ErrorCode.INVALID_INPUT);
        }

        ResearchMetric metric = researchMetricRepository.findByProfessorId(professorId)
                .orElseGet(() -> researchMetricRepository.save(ResearchMetric.create(professor)));

        metric.assignAuthorManually(shortAuthorId);
        researchMetricSyncService.refreshMetrics(metric);

        return toResponse(professor, metric);
    }

    private ResearchMetricResponseDto toResponse(Professor professor, ResearchMetric metric) {
        String fromYear = String.valueOf(Year.now().getValue() - (RECENT_YEARS - 1));
        long recentPublicationCount = publicationRepository.countByProfessorIdAndYearGreaterThanEqual(professor.getId(), fromYear);

        return ResearchMetricResponseDto.of(professor, metric, recentPublicationCount);
    }
}
