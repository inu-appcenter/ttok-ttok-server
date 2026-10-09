package com.inuappcenter.team_2_project_server.ai;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.publication.entity.Publication;
import com.inuappcenter.team_2_project_server.domain.publication.repository.PublicationRepository;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.LabCandidateContextRow;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.LabCandidateRow;
import com.inuappcenter.team_2_project_server.domain.ai.dto.row.PublicationSourceRow;
import com.inuappcenter.team_2_project_server.domain.ai.entity.LabSearchDocument;
import com.inuappcenter.team_2_project_server.domain.ai.enums.SearchSourceType;
import com.inuappcenter.team_2_project_server.domain.ai.repository.LabSearchDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 검색 SQL(pgvector)이 실제 PostgreSQL에서 의도한 점수와 순서를 내는지 검증한다.
 * 벡터를 손으로 정해서(질문 벡터 = 첫 번째 축) 각 문서의 코사인 유사도를 미리 알 수 있게 만든다.
 */
@SpringBootTest
@ActiveProfiles("test")
class LabSearchDocumentRepositoryIntegrationTest {

    // 질문 벡터: 첫 번째 축 방향
    private static final String QUERY_VECTOR = vectorLiteral(1, 0);
    private final List<Laboratory> laboratories = new ArrayList<>();
    private final List<Professor> professors = new ArrayList<>();
    @Autowired
    private LabSearchDocumentRepository labSearchDocumentRepository;
    @Autowired
    private LaboratoryRepository laboratoryRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private PublicationRepository publicationRepository;
    private Laboratory profileOnlyLab;
    private Laboratory paperStrongLab;
    private Laboratory paperOnlyLab;

    // 코사인 유사도가 x인 벡터: (x, sqrt(1 - x^2), 0, 0, ...)
    private static float[] vectorWithSimilarity(double similarity) {
        float[] vector = new float[LabSearchDocument.EMBEDDING_DIMENSIONS];
        vector[0] = (float) similarity;
        vector[1] = (float) Math.sqrt(1 - similarity * similarity);
        return vector;
    }

    private static String vectorLiteral(float first, float second) {
        StringBuilder literal = new StringBuilder("[").append(first).append(",").append(second);
        for (int i = 2; i < LabSearchDocument.EMBEDDING_DIMENSIONS; i++) {
            literal.append(",0");
        }
        return literal.append("]").toString();
    }

    @BeforeEach
    void setUp() {
        // 프로필 유사도 0.8, 논문 없음 -> 점수 0.8
        profileOnlyLab = laboratory("프로필만강한연구실");
        document(profileOnlyLab, SearchSourceType.PROFILE, profileOnlyLab.getId(), "프로필A", 0.8);

        // 프로필 0.6, 논문 1.0 / 1.0 / 1.0 / 0.0 -> 가까운 3편 평균 1.0 -> 0.7 x 0.6 + 0.3 x 1.0 = 0.72
        paperStrongLab = laboratory("논문이강한연구실");
        document(paperStrongLab, SearchSourceType.PROFILE, paperStrongLab.getId(), "프로필B", 0.6);
        document(paperStrongLab, SearchSourceType.PUBLICATION, 101L, "가까운논문1", 1.0);
        document(paperStrongLab, SearchSourceType.PUBLICATION, 102L, "가까운논문2", 1.0);
        document(paperStrongLab, SearchSourceType.PUBLICATION, 103L, "가까운논문3", 1.0);
        document(paperStrongLab, SearchSourceType.PUBLICATION, 104L, "먼논문", 0.0);

        // 프로필 0.0, 논문 1.0 x 3 -> 0.7 x 0 + 0.3 x 1.0 = 0.3
        paperOnlyLab = laboratory("논문만강한연구실");
        document(paperOnlyLab, SearchSourceType.PROFILE, paperOnlyLab.getId(), "프로필C", 0.0);
        document(paperOnlyLab, SearchSourceType.PUBLICATION, 201L, "논문C1", 1.0);
        document(paperOnlyLab, SearchSourceType.PUBLICATION, 202L, "논문C2", 1.0);
        document(paperOnlyLab, SearchSourceType.PUBLICATION, 203L, "논문C3", 1.0);
    }

    @AfterEach
    void tearDown() {
        labSearchDocumentRepository.deleteAll();
        publicationRepository.deleteAll();
        laboratoryRepository.deleteAll(laboratories);
        professorRepository.deleteAll(professors);
    }

    @Test
    void candidates_are_ranked_by_profile_70_percent_plus_top3_papers_30_percent() {
        List<LabCandidateRow> candidates = labSearchDocumentRepository.findCandidates(QUERY_VECTOR, 0.7, 3, 20);

        assertThat(candidates).extracting(LabCandidateRow::getLaboratoryId)
                .containsExactly(profileOnlyLab.getId(), paperStrongLab.getId(), paperOnlyLab.getId());
        assertThat(candidates).extracting(LabCandidateRow::getScore)
                .satisfiesExactly(
                        score -> assertThat(score).isCloseTo(0.8, within(1e-4)),
                        score -> assertThat(score).isCloseTo(0.72, within(1e-4)),
                        score -> assertThat(score).isCloseTo(0.3, within(1e-4))
                );
    }

    @Test
    void candidate_count_limits_results() {
        assertThat(labSearchDocumentRepository.findCandidates(QUERY_VECTOR, 0.7, 3, 2)).hasSize(2);
    }

    @Test
    void contexts_have_profile_and_closest_papers_only() {
        List<LabCandidateContextRow> contexts = labSearchDocumentRepository.findCandidateContexts(
                QUERY_VECTOR, List.of(paperStrongLab.getId()), 2);

        assertThat(contexts).extracting(LabCandidateContextRow::getSourceType, LabCandidateContextRow::getContent)
                .hasSize(3)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("PROFILE", "프로필B")
                )
                .doesNotContain(org.assertj.core.groups.Tuple.tuple("PUBLICATION", "먼논문"));
        assertThat(contexts).allSatisfy(row -> assertThat(row.getLaboratoryId()).isEqualTo(paperStrongLab.getId()));
    }

    @Test
    void stored_vector_is_read_back_as_float_array() {
        LabSearchDocument document = labSearchDocumentRepository.findAll().getFirst();

        assertThat(document.getEmbedding()).hasSize(LabSearchDocument.EMBEDDING_DIMENSIONS);
    }

    @Test
    void deleting_laboratory_deletes_its_documents() {
        laboratoryRepository.delete(profileOnlyLab);
        laboratories.remove(profileOnlyLab);

        assertThat(labSearchDocumentRepository.findAllKeys())
                .noneMatch(row -> row.getSourceType() == SearchSourceType.PROFILE && row.getSourceId().equals(profileOnlyLab.getId()));
        // 문서 10개(1 + 5 + 4) 중 삭제한 연구실의 프로필 1개만 지워진다
        assertThat(labSearchDocumentRepository.findAllKeys()).hasSize(9);
    }

    @Test
    void publication_sources_include_only_publications_linked_to_laboratory() {
        Publication linked = publicationRepository.save(Publication.create(
                paperStrongLab, paperStrongLab.getProfessor(), "연결된 논문", null, "IEEE", "2025", null, null, null, null));
        publicationRepository.save(Publication.create(
                null, null, "연구실 없는 논문", null, null, "2025", null, null, null, null));

        List<PublicationSourceRow> sources = labSearchDocumentRepository.findPublicationSources();

        assertThat(sources).extracting(PublicationSourceRow::getId, PublicationSourceRow::getLaboratoryId, PublicationSourceRow::getTitle, PublicationSourceRow::getPlatform)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(linked.getId(), paperStrongLab.getId(), "연결된 논문", "IEEE"));
    }

    private Laboratory laboratory(String labName) {
        Professor professor = professorRepository.save(Professor.create(labName + "교수", "교수", Department.COMPUTER_ENGINEERING, null, null));
        professors.add(professor);
        Laboratory laboratory = laboratoryRepository.save(Laboratory.create(
                professor.getCollege(), Department.COMPUTER_ENGINEERING, labName, null, 0, 0, null, professor, null, null
        ));
        laboratories.add(laboratory);
        return laboratory;
    }

    private void document(Laboratory laboratory, SearchSourceType sourceType, Long sourceId, String content, double similarity) {
        labSearchDocumentRepository.save(LabSearchDocument.create(
                laboratory, sourceType, sourceId, content, "hash-" + content, "test-model", vectorWithSimilarity(similarity)
        ));
    }
}
