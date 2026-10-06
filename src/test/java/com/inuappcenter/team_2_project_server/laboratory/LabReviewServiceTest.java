package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewOptionsResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewRequestDto;
import com.inuappcenter.team_2_project_server.domain.labReview.dto.LabReviewResponseDto;
import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;
import com.inuappcenter.team_2_project_server.domain.labReview.repository.LabReviewRepository;
import com.inuappcenter.team_2_project_server.domain.labReview.service.LabReviewService;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class LabReviewServiceTest {

    private LabReviewRepository labReviewRepository;
    private ResearcherRepository researcherRepository;
    private LabReviewService labReviewService;

    private Laboratory laboratory;
    private Researcher researcher;

    private static Set<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @BeforeEach
    void setUp() {
        labReviewRepository = mock(LabReviewRepository.class);
        researcherRepository = mock(ResearcherRepository.class);
        labReviewService = new LabReviewService(labReviewRepository, researcherRepository);

        laboratory = mock(Laboratory.class);
        given(laboratory.getId()).willReturn(10L);
        given(laboratory.getLabName()).willReturn("소프트웨어공학 연구실");

        researcher = mock(Researcher.class);
        given(researcher.getId()).willReturn(5L);
        given(researcher.getLaboratory()).willReturn(laboratory);
    }

    private LabReviewRequestDto request(String coreTime, String weeklyMeeting, String... doings) {
        return new LabReviewRequestDto(coreTime, weeklyMeeting, set(doings));
    }

    @Test
    void submit_succeeds_and_normalizes_input() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.existsByResearcherId(5L)).willReturn(false);
        given(labReviewRepository.save(any(LabReview.class))).willAnswer(i -> i.getArgument(0));

        LabReviewResponseDto response = labReviewService.submit(
                1L, request(" 있음 ", "주  1회", " 논문  리딩 ", "실험/코딩"));

        assertThat(response.coreTime()).isEqualTo("있음");
        assertThat(response.weeklyMeeting()).isEqualTo("주 1회");
        assertThat(response.doings()).containsExactly("논문 리딩", "실험/코딩");
        assertThat(response.laboratoryId()).isEqualTo(10L);
        verify(labReviewRepository).save(any(LabReview.class));
    }

    @Test
    void submit_fails_when_not_researcher() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> labReviewService.submit(1L, request("있음", "주 1회", "논문 리딩")))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESEARCHER_NOT_FOUND);

        verify(labReviewRepository, never()).save(any());
    }

    @Test
    void submit_fails_when_review_already_exists() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.existsByResearcherId(5L)).willReturn(true);

        assertThatThrownBy(() -> labReviewService.submit(1L, request("있음", "주 1회", "논문 리딩")))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LAB_REVIEW_ALREADY_EXISTS);

        verify(labReviewRepository, never()).save(any());
    }

    @Test
    void submit_translates_unique_violation_to_domain_error() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.existsByResearcherId(5L)).willReturn(false);
        given(labReviewRepository.save(any(LabReview.class)))
                .willThrow(new DataIntegrityViolationException("duplicate researcher_id"));

        assertThatThrownBy(() -> labReviewService.submit(1L, request("있음", "주 1회", "논문 리딩")))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LAB_REVIEW_ALREADY_EXISTS);
    }

    @Test
    void getOptions_returns_default_lists() {
        LabReviewOptionsResponseDto options = labReviewService.getOptions();

        assertThat(options.coreTime()).isEqualTo(LabReview.CORE_TIME);
        assertThat(options.weeklyMeeting()).isEqualTo(LabReview.WEEKLY_MEETING);
        assertThat(options.works()).isEqualTo(LabReview.WORKS);
        assertThat(options.works()).isNotEmpty();
    }

    @Test
    void getLabReviews_maps_every_row() {
        LabReview r1 = LabReview.create(laboratory, researcher, "있음", "주 1회", set("논문 리딩"));
        LabReview r2 = LabReview.create(laboratory, researcher, "없음", "격주", set("데이터 라벨링"));
        given(labReviewRepository.findAllByLaboratoryId(10L)).willReturn(List.of(r1, r2));

        List<LabReviewResponseDto> result = labReviewService.getLabReviews(10L);

        assertThat(result).extracting(LabReviewResponseDto::coreTime).containsExactly("있음", "없음");
    }

    @Test
    void getMyLabReview_returns_null_when_absent() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.empty());

        assertThat(labReviewService.getMyLabReview(1L)).isNull();
    }

    @Test
    void getMyLabReview_returns_dto_when_present() {
        LabReview review = LabReview.create(laboratory, researcher, "있음", "주 1회", set("논문 리딩"));
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.of(review));

        assertThat(labReviewService.getMyLabReview(1L).coreTime()).isEqualTo("있음");
    }

    @Test
    void updateMyLabReview_normalizes_and_mutates_entity() {
        LabReview review = LabReview.create(laboratory, researcher, "예전", "예전", set("예전"));
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.of(review));

        LabReviewResponseDto response = labReviewService.updateMyLabReview(
                1L, request(" 없음 ", "격주", " 세미나  발표 "));

        assertThat(review.getCoreTime()).isEqualTo("없음");
        assertThat(response.weeklyMeeting()).isEqualTo("격주");
        assertThat(response.doings()).containsExactly("세미나 발표");
    }

    @Test
    void updateMyLabReview_fails_when_review_absent() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> labReviewService.updateMyLabReview(1L, request("없음", "격주", "논문 리딩")))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LAB_REVIEW_NOT_FOUND);
    }

    @Test
    void deleteMyLabReview_deletes_and_returns_review_id() {
        LabReview review = mock(LabReview.class);
        given(review.getId()).willReturn(99L);
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.of(review));

        Long deletedId = labReviewService.deleteMyLabReview(1L);

        assertThat(deletedId).isEqualTo(99L);
        verify(labReviewRepository).delete(review);
    }

    @Test
    void deleteMyLabReview_fails_when_review_absent() {
        given(researcherRepository.findByMemberId(1L)).willReturn(Optional.of(researcher));
        given(labReviewRepository.findByResearcherId(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> labReviewService.deleteMyLabReview(1L))
                .isInstanceOf(MyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LAB_REVIEW_NOT_FOUND);
    }
}
