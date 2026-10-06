package com.inuappcenter.team_2_project_server.domain.labReview.entity;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table
public class LabReview extends BaseEntity {

    public static final List<String> CORE_TIME = List.of("있음", "없음", "자율");
    public static final List<String> WEEKLY_MEETING = List.of("주 1회", "주 2회 이상", "격주", "월 1회", "비정기", "없음");
    public static final List<String> WORKS = List.of(
            "논문 리딩", "실험/코딩", "데이터 라벨링", "데이터 수집/전처리",
            "논문 작성", "프로젝트 개발", "세미나 발표", "코드 리뷰",
            "대회/공모전 참가", "장비/서버 관리", "학회 참석", "연구과제", "개인연구", "1:1미팅", "랩미팅");
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_review_id")
    private Long id;

    @JoinColumn(name = "laboratory_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Laboratory laboratory;

    @JoinColumn(name = "researcher_id", unique = true, nullable = false)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Researcher researcher;

    @Column(name = "core_time")
    private String coreTime;

    @Column(name = "weekly_meeting")
    private String weeklyMeeting;

    @ElementCollection
    @CollectionTable(name = "lab_review_doings",
            joinColumns = @JoinColumn(name = "lab_review_id"))
    @Column(name = "doings")
    private Set<String> doings = new LinkedHashSet<>();

    private LabReview(
            Laboratory laboratory,
            Researcher researcher,
            String coreTime,
            String weeklyMeeting,
            Set<String> doings
    ) {
        this.laboratory = laboratory;
        this.researcher = researcher;
        this.coreTime = coreTime;
        this.weeklyMeeting = weeklyMeeting;
        this.doings = doings;
    }

    public static LabReview create(
            Laboratory laboratory,
            Researcher researcher,
            String coreTime,
            String weeklyMeeting,
            Set<String> doings
    ) {
        return new LabReview(
                laboratory,
                researcher,
                normalize(coreTime),
                normalize(weeklyMeeting),
                normalizeAll(doings)
        );
    }

    // 자유 입력 문자열 정제: NFC 결합, 앞뒤 공백 제거, 내부 공백 1칸, 빈 문자열은 null
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .trim()
                .replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    // 컬렉션 각 원소를 정제하고 null/빈 값 제거
    private static Set<String> normalizeAll(Set<String> raw) {
        return (raw == null ? Set.<String>of() : raw).stream()
                .map(LabReview::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void update(
            String coreTime,
            String weeklyMeeting,
            Set<String> doings
    ) {
        this.coreTime = normalize(coreTime);
        this.weeklyMeeting = normalize(weeklyMeeting);
        this.doings = normalizeAll(doings);
    }
}
