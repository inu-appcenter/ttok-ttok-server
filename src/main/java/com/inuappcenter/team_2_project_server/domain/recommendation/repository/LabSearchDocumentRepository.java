package com.inuappcenter.team_2_project_server.domain.recommendation.repository;

import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabCandidateContextRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabCandidateRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.LabSearchDocumentKeyRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.dto.row.PublicationSourceRow;
import com.inuappcenter.team_2_project_server.domain.recommendation.entity.LabSearchDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LabSearchDocumentRepository extends JpaRepository<LabSearchDocument, Long> {

    // 인덱싱: 이미 저장된 문서와 원본을 비교하기 위한 요약 (벡터는 읽지 않음)
    @Query("""
            select d.id as id, d.sourceType as sourceType, d.sourceId as sourceId,
                   d.contentHash as contentHash, d.embeddingModel as embeddingModel
            from LabSearchDocument d
            """)
    List<LabSearchDocumentKeyRow> findAllKeys();

    // 인덱싱 원본: 연구실에 연결된 논문 (검색에 쓰는 제목, 게재처만)
    @Query("""
            select p.id as id, p.laboratory.id as laboratoryId, p.title as title, p.platform as platform
            from Publication p
            where p.laboratory is not null and p.title is not null and p.title <> ''
            """)
    List<PublicationSourceRow> findPublicationSources();

    /**
     * 검색 1단계: 질문 벡터와 가까운 연구실 후보를 점수 순으로 찾는다.
     * 점수 = 프로필 유사도 x profileWeight + 질문과 가장 가까운 논문 paperTopN편의 평균 유사도 x (1 - profileWeight)
     * 논문이 없는 연구실은 프로필 유사도만 쓴다.
     * (inu-rag/eval 실험의 hybrid_p7 방식. 평가셋에서 가장 점수가 높았던 조합)
     * <p>
     * - <=> 는 pgvector의 코사인 거리 연산자이고, 유사도 = 1 - 거리
     * - 문서가 약 1만 개라 벡터 인덱스 없이 전체를 비교한다
     * - 질문 벡터는 '[0.1,0.2,...]' 형태 문자열로 받아 vector로 변환한다
     */
    @Query(nativeQuery = true, value = """
            with scored as (
                select d.laboratory_id, d.source_type,
                       1 - (d.embedding <=> cast(:queryVector as vector)) as similarity
                from lab_search_document d
            ),
            paper_ranked as (
                select laboratory_id, similarity,
                       row_number() over (partition by laboratory_id order by similarity desc) as rn
                from scored
                where source_type = 'PUBLICATION'
            ),
            paper as (
                select laboratory_id, avg(similarity) as similarity
                from paper_ranked
                where rn <= :paperTopN
                group by laboratory_id
            )
            select profile.laboratory_id as "laboratoryId",
                   case when paper.similarity is null then profile.similarity
                        else :profileWeight * profile.similarity + (1 - :profileWeight) * paper.similarity
                   end as "score"
            from scored profile
            left join paper on paper.laboratory_id = profile.laboratory_id
            where profile.source_type = 'PROFILE'
            order by "score" desc
            limit :candidateCount
            """)
    List<LabCandidateRow> findCandidates(
            @Param("queryVector") String queryVector,
            @Param("profileWeight") double profileWeight,
            @Param("paperTopN") int paperTopN,
            @Param("candidateCount") int candidateCount
    );

    /**
     * 검색 2단계(LLM 선택)에 넘길 후보 연구실 정보.
     * 연구실마다 프로필 1개와, 그 연구실 논문 중 질문과 가장 가까운 paperCount편을 가까운 순으로 돌려준다.
     * (요약에는 없고 논문에만 근거가 있는 연구실을 LLM이 알아보게 하기 위함)
     */
    @Query(nativeQuery = true, value = """
            select ranked.laboratory_id as "laboratoryId", ranked.source_type as "sourceType", ranked.content as "content"
            from (
                select d.laboratory_id, d.source_type, d.content,
                       row_number() over (
                           partition by d.laboratory_id, d.source_type
                           order by d.embedding <=> cast(:queryVector as vector)
                       ) as rn
                from lab_search_document d
                where d.laboratory_id in (:laboratoryIds)
            ) ranked
            where ranked.source_type = 'PROFILE' or ranked.rn <= :paperCount
            order by ranked.laboratory_id, ranked.source_type, ranked.rn
            """)
    List<LabCandidateContextRow> findCandidateContexts(
            @Param("queryVector") String queryVector,
            @Param("laboratoryIds") Collection<Long> laboratoryIds,
            @Param("paperCount") int paperCount
    );
}
