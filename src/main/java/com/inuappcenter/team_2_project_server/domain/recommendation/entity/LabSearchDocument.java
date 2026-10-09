package com.inuappcenter.team_2_project_server.domain.recommendation.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.recommendation.enums.SearchSourceType;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

/**
 * 연구실 추천 검색 문서 (임베딩 벡터 저장)
 * - 연구실 프로필과 논문을 문서 하나씩으로 임베딩해 두고, 추천 요청 때 질문 벡터와의 거리로 후보 연구실을 찾는다.
 * - 원본(Laboratory, Publication)에서 언제든 다시 만들 수 있는 파생 데이터라 이력 보존 없이 물리 삭제한다.
 * - 연구실이 삭제되면 DB의 FK(on delete cascade)가 이 문서도 함께 지운다. 연구실 삭제 로직을 건드리지 않기 위함
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lab_search_document",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lab_search_document_source",
                        columnNames = {"source_type", "source_id"}
                )
        },
        indexes = {
                @Index(name = "idx_lab_search_document_laboratory", columnList = "laboratory_id")
        }
)
public class LabSearchDocument extends BaseEntity {

    // text-embedding-3-small의 벡터 크기. 임베딩 모델을 바꾸면 컬럼 크기도 바꾸고 전체를 다시 임베딩해야 한다
    public static final int EMBEDDING_DIMENSIONS = 1536;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_search_document_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Laboratory laboratory;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SearchSourceType sourceType;

    // PROFILE이면 연구실 id, PUBLICATION이면 논문 id
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    // 임베딩한 원문. 추천 때 LLM 프롬프트에도 그대로 들어간다
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 원문의 SHA-256. 인덱싱 때 원문이 바뀐 문서만 다시 임베딩하려고 비교한다
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    // 어떤 모델로 만든 벡터인지. 모델이 다른 벡터끼리는 거리를 비교할 수 없어서 기록한다
    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSIONS)
    @Column(name = "embedding", nullable = false)
    private float[] embedding;

    private LabSearchDocument(
            Laboratory laboratory,
            SearchSourceType sourceType,
            Long sourceId,
            String content,
            String contentHash,
            String embeddingModel,
            float[] embedding
    ) {
        this.laboratory = laboratory;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.content = content;
        this.contentHash = contentHash;
        this.embeddingModel = embeddingModel;
        this.embedding = embedding;
    }

    public static LabSearchDocument create(
            Laboratory laboratory,
            SearchSourceType sourceType,
            Long sourceId,
            String content,
            String contentHash,
            String embeddingModel,
            float[] embedding
    ) {
        return new LabSearchDocument(laboratory, sourceType, sourceId, content, contentHash, embeddingModel, embedding);
    }

    // 원문이 바뀌었거나 임베딩 모델이 바뀌어 다시 임베딩한 결과 반영
    public void updateContent(String content, String contentHash, String embeddingModel, float[] embedding) {
        this.content = content;
        this.contentHash = contentHash;
        this.embeddingModel = embeddingModel;
        this.embedding = embedding;
    }
}
