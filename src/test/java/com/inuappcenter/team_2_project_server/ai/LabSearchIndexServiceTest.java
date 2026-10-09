package com.inuappcenter.team_2_project_server.ai;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.ai.client.FactChatClient;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.LabSearchDocumentKeyRow;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.PublicationSourceRow;
import com.inuappcenter.team_2_project_server.domain.ai.entity.LabSearchDocument;
import com.inuappcenter.team_2_project_server.domain.ai.enums.SearchSourceType;
import com.inuappcenter.team_2_project_server.domain.ai.repository.LabSearchDocumentRepository;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.LabSearchDocumentTexts;
import com.inuappcenter.team_2_project_server.domain.ai.service.rag.LabSearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class LabSearchIndexServiceTest {

    private static final String MODEL = "text-embedding-3-small";
    private final List<Laboratory> laboratories = new ArrayList<>();
    private final List<PublicationSourceRow> publications = new ArrayList<>();
    private final List<LabSearchDocumentKeyRow> savedDocuments = new ArrayList<>();
    private LabSearchDocumentRepository labSearchDocumentRepository;
    private FactChatClient factChatClient;
    private LabSearchIndexService labSearchIndexService;

    @BeforeEach
    void setUp() {
        LaboratoryRepository laboratoryRepository = mock(LaboratoryRepository.class);
        labSearchDocumentRepository = mock(LabSearchDocumentRepository.class);
        factChatClient = mock(FactChatClient.class);
        labSearchIndexService = new LabSearchIndexService(
                laboratoryRepository,
                labSearchDocumentRepository,
                factChatClient,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                MODEL
        );

        given(laboratoryRepository.findAllBy()).willReturn(laboratories);
        given(labSearchDocumentRepository.findPublicationSources()).willReturn(publications);
        given(labSearchDocumentRepository.findAllKeys()).willReturn(savedDocuments);
        // 보낸 문장 수만큼 1536차원 벡터를 돌려주는 임베딩 API
        given(factChatClient.embed(eq(MODEL), anyList())).willAnswer(invocation -> {
            List<String> texts = invocation.getArgument(1);
            return Optional.of(texts.stream().map(text -> new float[LabSearchDocument.EMBEDDING_DIMENSIONS]).toList());
        });
    }

    @Test
    void embeds_and_saves_new_profile_and_publication_documents() {
        Laboratory laboratory = laboratory(1L, "AI연구실", "머신러닝,컴퓨터비전", "딥러닝을 연구합니다.");
        publication(10L, 1L, "Deep Learning", "IEEE TPAMI");

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result).isEqualTo(new LabSearchIndexSyncResponseDto(2, 0, 0, 0, 0));
        ArgumentCaptor<LabSearchDocument> saved = ArgumentCaptor.forClass(LabSearchDocument.class);
        verify(labSearchDocumentRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(LabSearchDocument::getSourceType, LabSearchDocument::getSourceId, LabSearchDocument::getContent)
                .containsExactly(
                        tuple(SearchSourceType.PROFILE, 1L, LabSearchDocumentTexts.profile(laboratory)),
                        tuple(SearchSourceType.PUBLICATION, 10L, "Deep Learning. IEEE TPAMI")
                );
        assertThat(saved.getAllValues()).allSatisfy(document -> {
            assertThat(document.getEmbeddingModel()).isEqualTo(MODEL);
            assertThat(document.getContentHash()).isEqualTo(LabSearchDocumentTexts.hash(document.getContent()));
        });
    }

    @Test
    void profile_text_has_lab_department_professor_fields_and_introduction() {
        Laboratory laboratory = laboratory(1L, "AI연구실", "머신러닝,컴퓨터비전", "  딥러닝을 연구합니다.  ");

        assertThat(LabSearchDocumentTexts.profile(laboratory)).isEqualTo("""
                AI연구실 (컴퓨터공학부, 홍길동 교수)
                연구분야: 머신러닝,컴퓨터비전
                딥러닝을 연구합니다.""");
    }

    @Test
    void skips_documents_whose_content_and_model_are_unchanged() {
        Laboratory laboratory = laboratory(1L, "AI연구실", null, null);
        saved(100L, SearchSourceType.PROFILE, 1L, LabSearchDocumentTexts.profile(laboratory), MODEL);

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result).isEqualTo(new LabSearchIndexSyncResponseDto(0, 0, 0, 0, 1));
        verify(factChatClient, never()).embed(anyString(), anyList());
    }

    @Test
    void re_embeds_document_whose_content_changed() {
        laboratory(1L, "AI연구실", null, "바뀐 소개");
        saved(100L, SearchSourceType.PROFILE, 1L, "예전 원문", MODEL);
        LabSearchDocument document = mock(LabSearchDocument.class);
        given(labSearchDocumentRepository.findById(100L)).willReturn(Optional.of(document));

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result.updated()).isEqualTo(1);
        verify(document).updateContent(contains("바뀐 소개"), anyString(), eq(MODEL), any(float[].class));
        verify(labSearchDocumentRepository, never()).save(any());
    }

    @Test
    void re_embeds_document_made_with_another_embedding_model() {
        Laboratory laboratory = laboratory(1L, "AI연구실", null, null);
        saved(100L, SearchSourceType.PROFILE, 1L, LabSearchDocumentTexts.profile(laboratory), "old-model");
        given(labSearchDocumentRepository.findById(100L)).willReturn(Optional.of(mock(LabSearchDocument.class)));

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result.updated()).isEqualTo(1);
    }

    @Test
    void deletes_documents_whose_source_no_longer_exists() {
        Laboratory laboratory = laboratory(1L, "AI연구실", null, null);
        saved(100L, SearchSourceType.PROFILE, 1L, LabSearchDocumentTexts.profile(laboratory), MODEL);
        saved(200L, SearchSourceType.PUBLICATION, 55L, "삭제된 논문", MODEL);

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result.deleted()).isEqualTo(1);
        verify(labSearchDocumentRepository).deleteAllByIdInBatch(List.of(200L));
    }

    @Test
    void counts_failed_and_saves_nothing_when_embedding_api_fails() {
        laboratory(1L, "AI연구실", null, null);
        given(factChatClient.embed(anyString(), anyList())).willReturn(Optional.empty());

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result).isEqualTo(new LabSearchIndexSyncResponseDto(0, 0, 0, 1, 0));
        verify(labSearchDocumentRepository, never()).save(any());
    }

    @Test
    void counts_failed_when_vector_dimension_is_wrong() {
        laboratory(1L, "AI연구실", null, null);
        given(factChatClient.embed(anyString(), anyList())).willReturn(Optional.of(List.of(new float[3])));

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result.failed()).isEqualTo(1);
        verify(labSearchDocumentRepository, never()).save(any());
    }

    @Test
    void embeds_in_batches_of_100() {
        laboratory(1L, "AI연구실", null, null);
        for (long id = 1; id <= 149; id++) {
            publication(id, 1L, "논문" + id, null);
        }

        LabSearchIndexSyncResponseDto result = labSearchIndexService.syncAll();

        assertThat(result.created()).isEqualTo(150);
        ArgumentCaptor<List<String>> batches = ArgumentCaptor.forClass(List.class);
        verify(factChatClient, times(2)).embed(eq(MODEL), batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(100, 50);
    }

    private Laboratory laboratory(Long id, String labName, String researchFieldRaw, String introduction) {
        Professor professor = Professor.create("홍길동", "교수", Department.COMPUTER_ENGINEERING, null, null);
        Laboratory laboratory = Laboratory.create(
                professor.getCollege(), Department.COMPUTER_ENGINEERING, labName, null, 0, 0, introduction, professor, null, researchFieldRaw
        );
        ReflectionTestUtils.setField(laboratory, "id", id);
        laboratories.add(laboratory);
        return laboratory;
    }

    private void publication(Long id, Long laboratoryId, String title, String platform) {
        publications.add(new PublicationRow(id, laboratoryId, title, platform));
    }

    private void saved(Long id, SearchSourceType sourceType, Long sourceId, String content, String model) {
        savedDocuments.add(new KeyRow(id, sourceType, sourceId, LabSearchDocumentTexts.hash(content), model));
    }

    private record PublicationRow(Long getId, Long getLaboratoryId, String getTitle, String getPlatform) implements PublicationSourceRow {
    }

    private record KeyRow(Long getId, SearchSourceType getSourceType, Long getSourceId, String getContentHash,
                          String getEmbeddingModel) implements LabSearchDocumentKeyRow {
    }
}
