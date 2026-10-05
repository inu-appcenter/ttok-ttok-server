package com.inuappcenter.team_2_project_server.domain.laboratory.service;

import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 동명이인 교수의 NTIS 과제가 어느 교수의 과제인지 판별한다. (동기화 1회 실행마다 새로 만들어 쓴다)
 * NTIS 응답에는 연구책임자 이름만 있고 학과 같은 식별 정보가 없어서, 아래 근거로 후보 교수마다 점수를 매긴다.
 * - 주 근거: 과제의 과학기술표준분류 대분류가 후보 교수 학과의 분류 분포와 얼마나 맞는지
 *   (학과별 분류 분포는 동명이인이 아닌 같은 학과 교수들의 과제 분류로 만든다 → 학과-분류 대응표를 따로 관리하지 않음)
 * - 보조 근거: 과제명/키워드에 후보 연구실의 연구분야가 들어 있는지
 * 1위가 2위보다 확실히 앞설 때만 매핑하고, 애매하면 보류한다.
 */
class ResearchProjectOwnerResolver {

    // 신분류 1~3순위 대분류 가중치 (1순위가 과제의 주 분야)
    private static final int[] SEQUENCE_WEIGHTS = {3, 2, 1};

    private static final double RESEARCH_AREA_MATCH_SCORE = 1.0;
    private static final double MAX_RESEARCH_AREA_SCORE = 2.0;

    // 1위 점수가 이 값 이상이고, 2위의 MIN_LEAD_RATIO배 이상일 때만 매핑
    private static final double MIN_SCORE = 1.0;
    private static final double MIN_LEAD_RATIO = 2.0;

    private static final Pattern RESEARCH_AREA_DELIMITER = Pattern.compile("[,;/·\\n]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    // "AI", "IT" 같은 짧은 단어는 과제명에 우연히 포함되는 경우가 많아 2글자 이상만 비교
    private static final int MIN_RESEARCH_AREA_LENGTH = 2;

    private final Map<Department, Map<String, Integer>> classCountsByDepartment = new EnumMap<>(Department.class);

    record Candidate(Professor professor, Laboratory laboratory) {
    }

    // owner가 null이면 판별 보류. owner의 laboratory가 null이면 연구실이 없는 동명이인의 과제
    record Decision(Candidate owner) {
        boolean isHeld() {
            return owner == null;
        }
    }

    // 동명이인이 아닌 교수의 과제 분류를 학과별로 쌓는다 (resolve 전에 호출)
    void learn(Department department, List<String> scienceClassLarges) {
        if (department == null || scienceClassLarges.isEmpty()) {
            return;
        }
        Map<String, Integer> counts = classCountsByDepartment.computeIfAbsent(department, d -> new HashMap<>());
        for (String large : scienceClassLarges) {
            counts.merge(large, 1, Integer::sum);
        }
    }

    Decision resolve(List<String> scienceClassLarges, String projectText, List<Candidate> candidates) {
        List<Map.Entry<Candidate, Double>> ranked = candidates.stream()
                .map(candidate -> Map.entry(candidate, score(scienceClassLarges, projectText, candidate)))
                .sorted(Map.Entry.<Candidate, Double>comparingByValue(Comparator.reverseOrder()))
                .toList();

        if (ranked.isEmpty()) {
            return new Decision(null);
        }

        double best = ranked.get(0).getValue();
        double second = ranked.size() > 1 ? ranked.get(1).getValue() : 0;
        if (best < MIN_SCORE || best < second * MIN_LEAD_RATIO) {
            return new Decision(null);
        }
        return new Decision(ranked.get(0).getKey());
    }

    private double score(List<String> scienceClassLarges, String projectText, Candidate candidate) {
        return scienceClassScore(scienceClassLarges, candidate.professor().getDepartment())
                + researchAreaScore(projectText, candidate.laboratory());
    }

    // 과제의 n순위 대분류가 학과 과제 중 차지하는 비율 × 순위 가중치의 합
    private double scienceClassScore(List<String> scienceClassLarges, Department department) {
        Map<String, Integer> counts = department == null ? null : classCountsByDepartment.get(department);
        if (counts == null || counts.isEmpty()) {
            return 0;
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        double score = 0;
        for (int i = 0; i < Math.min(scienceClassLarges.size(), SEQUENCE_WEIGHTS.length); i++) {
            score += SEQUENCE_WEIGHTS[i] * counts.getOrDefault(scienceClassLarges.get(i), 0) / (double) total;
        }
        return score;
    }

    // 과제명/키워드에 연구실 연구분야가 포함된 개수만큼 가점 (공백·대소문자 무시)
    private double researchAreaScore(String projectText, Laboratory laboratory) {
        if (laboratory == null || laboratory.getResearchFieldRaw() == null || projectText == null) {
            return 0;
        }

        String normalizedText = normalize(projectText);
        long matched = Arrays.stream(RESEARCH_AREA_DELIMITER.split(laboratory.getResearchFieldRaw()))
                .map(this::normalize)
                .filter(area -> area.length() >= MIN_RESEARCH_AREA_LENGTH)
                .distinct()
                .filter(normalizedText::contains)
                .count();
        return Math.min(matched * RESEARCH_AREA_MATCH_SCORE, MAX_RESEARCH_AREA_SCORE);
    }

    private String normalize(String value) {
        return WHITESPACE.matcher(value).replaceAll("").toLowerCase();
    }
}
