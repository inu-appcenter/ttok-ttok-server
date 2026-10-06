package com.inuappcenter.team_2_project_server.laboratory;

import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LabReviewNormalizeTest {

    @Test
    void normalize_returns_null_for_null_or_blank() {
        assertThat(LabReview.normalize(null)).isNull();
        assertThat(LabReview.normalize("   ")).isNull();
        assertThat(LabReview.normalize("\t\n")).isNull();
    }

    @Test
    void normalize_trims_and_collapses_inner_whitespace() {
        assertThat(LabReview.normalize("  논문  리딩 ")).isEqualTo("논문 리딩");
        assertThat(LabReview.normalize("논문\t\n리딩")).isEqualTo("논문 리딩");
    }

    @Test
    void normalize_applies_nfc_composition() {
        String decomposed = Normalizer.normalize("논문 리딩", Normalizer.Form.NFD);

        // 분해형(NFD)은 조합형보다 코드 유닛이 많다
        assertThat(decomposed.length()).isGreaterThan("논문 리딩".length());
        assertThat(LabReview.normalize(decomposed)).isEqualTo("논문 리딩");
    }

    @Test
    void create_normalizes_all_fields_and_drops_blank_doings() {
        Set<String> doings = new LinkedHashSet<>(Arrays.asList("논문 리딩", "  ", " 실험/코딩 "));

        LabReview review = LabReview.create(null, null, " 있음 ", "주  1회", doings);

        assertThat(review.getCoreTime()).isEqualTo("있음");
        assertThat(review.getWeeklyMeeting()).isEqualTo("주 1회");
        assertThat(review.getDoings()).containsExactly("논문 리딩", "실험/코딩");
    }

    @Test
    void create_treats_null_doings_as_empty_set() {
        LabReview review = LabReview.create(null, null, "있음", "주 1회", null);

        assertThat(review.getDoings()).isNotNull().isEmpty();
    }

    @Test
    void update_normalizes_like_create() {
        LabReview review = LabReview.create(null, null, "예전", "예전", Set.of("예전"));

        review.update(" 없음 ", "격주", new LinkedHashSet<>(Arrays.asList(" 세미나  발표 ", null)));

        assertThat(review.getCoreTime()).isEqualTo("없음");
        assertThat(review.getWeeklyMeeting()).isEqualTo("격주");
        assertThat(review.getDoings()).containsExactly("세미나 발표");
    }
}
