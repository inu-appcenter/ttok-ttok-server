package com.inuappcenter.team_2_project_server.domain.recommendation.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.recommendation.client.FactChatGatewayClient;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.response.LabSearchIndexSyncResponseDto;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabSearchDocumentKeyRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.PublicationSourceRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.entity.LabSearchDocument;
import com.inuappcenter.team_2_project_server.domain.recommendation.enums.SearchSourceType;
import com.inuappcenter.team_2_project_server.domain.recommendation.repository.LabSearchDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 연구실 프로필과 논문을 임베딩해 lab_search_document에 저장한다. (연구실 추천 검색의 준비 단계)
 * <p>
 * - 원문의 해시를 저장해 두고, 원문이 바뀌었거나 임베딩 모델이 바뀐 문서만 다시 임베딩한다. (처음 한 번만 전체 임베딩)
 * - 원본(연구실, 논문)이 없어진 문서는 지운다.
 * - 임베딩 API 호출은 트랜잭션 밖에서 하고, 저장은 묶음(100개) 단위로 커밋한다. (한 묶음이 실패해도 나머지는 진행)
 * - 실패한 문서는 저장하지 않으므로 다음 동기화 때 자연스럽게 다시 시도된다.
 */
@Slf4j
@Service
public class LabSearchIndexService {

    // 임베딩 API 한 번에 보낼 문서 수
    static final int EMBEDDING_BATCH_SIZE = 100;

    private final LaboratoryRepository laboratoryRepository;
    private final LabSearchDocumentRepository labSearchDocumentRepository;
    private final FactChatGatewayClient factChatGatewayClient;
    private final TransactionTemplate transactionTemplate;
    private final String embeddingModel;

    public LabSearchIndexService(
            LaboratoryRepository laboratoryRepository,
            LabSearchDocumentRepository labSearchDocumentRepository,
            FactChatGatewayClient factChatGatewayClient,
            TransactionTemplate transactionTemplate,
            @Value("${ai.recommendation.embedding-model:text-embedding-3-small}") String embeddingModel
    ) {
        this.laboratoryRepository = laboratoryRepository;
        this.labSearchDocumentRepository = labSearchDocumentRepository;
        this.factChatGatewayClient = factChatGatewayClient;
        this.transactionTemplate = transactionTemplate;
        this.embeddingModel = embeddingModel;
    }

    @Scheduled(cron = "0 0 5 * * *") // 매일 새벽 5시 (4시 NTIS, 4시 30분 OpenAlex 동기화와 겹치지 않게)
    public void scheduledSync() {
        syncAll();
    }

    public LabSearchIndexSyncResponseDto syncAll() {
        List<SourceDocument> sources = loadSourceDocuments();
        Map<DocumentKey, LabSearchDocumentKeyRow> existing = labSearchDocumentRepository.findAllKeys().stream()
                .collect(Collectors.toMap(row -> new DocumentKey(row.getSourceType(), row.getSourceId()), Function.identity()));
        log.info("연구실 검색 문서 동기화 시작 (원본 {}개, 저장된 문서 {}개)", sources.size(), existing.size());

        List<SourceDocument> toEmbed = sources.stream()
                .filter(source -> needsEmbedding(source, existing.get(source.key())))
                .toList();
        int unchanged = sources.size() - toEmbed.size();

        int created = 0;
        int updated = 0;
        int failed = 0;
        for (int start = 0; start < toEmbed.size(); start += EMBEDDING_BATCH_SIZE) {
            List<SourceDocument> batch = toEmbed.subList(start, Math.min(start + EMBEDDING_BATCH_SIZE, toEmbed.size()));
            try {
                int[] counts = embedAndSave(batch, existing);
                created += counts[0];
                updated += counts[1];
            } catch (Exception e) {
                // 한 묶음이 실패해도 나머지 묶음은 계속 진행
                log.error("연구실 검색 문서 저장 실패 ({}개)", batch.size(), e);
                failed += batch.size();
            }
        }

        Set<DocumentKey> sourceKeys = sources.stream().map(SourceDocument::key).collect(Collectors.toSet());
        List<Long> staleIds = existing.entrySet().stream()
                .filter(entry -> !sourceKeys.contains(entry.getKey()))
                .map(entry -> entry.getValue().getId())
                .toList();
        if (!staleIds.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> labSearchDocumentRepository.deleteAllByIdInBatch(staleIds));
        }

        LabSearchIndexSyncResponseDto result = new LabSearchIndexSyncResponseDto(created, updated, staleIds.size(), failed, unchanged);
        log.info("연구실 검색 문서 동기화 종료 {}", result);
        return result;
    }

    // 원문이 바뀌었거나, 아직 없거나, 다른 모델로 만든 벡터면 다시 임베딩한다
    private boolean needsEmbedding(SourceDocument source, LabSearchDocumentKeyRow saved) {
        return saved == null
                || !saved.getContentHash().equals(source.contentHash())
                || !saved.getEmbeddingModel().equals(embeddingModel);
    }

    /**
     * 한 묶음을 임베딩하고 저장한다.
     *
     * @return {새로 저장한 수, 갱신한 수}
     */
    private int[] embedAndSave(List<SourceDocument> batch, Map<DocumentKey, LabSearchDocumentKeyRow> existing) {
        List<float[]> vectors = factChatGatewayClient.embed(embeddingModel, batch.stream().map(SourceDocument::content).toList())
                .orElseThrow(() -> new IllegalStateException("임베딩 API 호출 실패"));
        for (float[] vector : vectors) {
            if (vector == null || vector.length != LabSearchDocument.EMBEDDING_DIMENSIONS) {
                throw new IllegalStateException("임베딩 벡터 크기가 " + LabSearchDocument.EMBEDDING_DIMENSIONS + "이 아님");
            }
        }

        return transactionTemplate.execute(status -> {
            int created = 0;
            int updated = 0;
            for (int i = 0; i < batch.size(); i++) {
                SourceDocument source = batch.get(i);
                float[] vector = vectors.get(i);
                LabSearchDocumentKeyRow saved = existing.get(source.key());

                if (saved == null) {
                    labSearchDocumentRepository.save(LabSearchDocument.create(
                            laboratoryRepository.getReferenceById(source.laboratoryId()),
                            source.sourceType(),
                            source.sourceId(),
                            source.content(),
                            source.contentHash(),
                            embeddingModel,
                            vector
                    ));
                    created++;
                } else {
                    labSearchDocumentRepository.findById(saved.getId()).ifPresent(document ->
                            document.updateContent(source.content(), source.contentHash(), embeddingModel, vector));
                    updated++;
                }
            }
            return new int[]{created, updated};
        });
    }

    // 원본 테이블에서 검색 문서로 만들 원문을 모은다
    private List<SourceDocument> loadSourceDocuments() {
        List<SourceDocument> sources = new ArrayList<>();

        for (Laboratory laboratory : laboratoryRepository.findAllBy()) {
            sources.add(SourceDocument.of(laboratory.getId(), SearchSourceType.PROFILE, laboratory.getId(),
                    LabSearchDocumentTexts.profile(laboratory)));
        }
        for (PublicationSourceRow publication : labSearchDocumentRepository.findPublicationSources()) {
            sources.add(SourceDocument.of(publication.getLaboratoryId(), SearchSourceType.PUBLICATION, publication.getId(),
                    LabSearchDocumentTexts.publication(publication.getTitle(), publication.getPlatform())));
        }
        return sources;
    }

    private record DocumentKey(SearchSourceType sourceType, Long sourceId) {
    }

    private record SourceDocument(
            Long laboratoryId,
            SearchSourceType sourceType,
            Long sourceId,
            String content,
            String contentHash
    ) {
        static SourceDocument of(Long laboratoryId, SearchSourceType sourceType, Long sourceId, String content) {
            return new SourceDocument(laboratoryId, sourceType, sourceId, content, LabSearchDocumentTexts.hash(content));
        }

        DocumentKey key() {
            return new DocumentKey(sourceType, sourceId);
        }
    }
}
