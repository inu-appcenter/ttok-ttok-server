package com.inuappcenter.team_2_project_server.domain.recommendation.service;

import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 검색 문서 원문 만들기. inu-rag/eval 실험(embedding_eval.py)과 같은 형식을 유지해야 실험 점수가 서버에서도 재현된다.
 */
public final class LabSearchDocumentTexts {

    private LabSearchDocumentTexts() {
    }

    /**
     * 연구실 프로필 원문
     * 예)
     * 멀티스케일아키텍처링연구실 (기계공학과, 김상문 교수)
     * 연구분야: 에너지변환장치(연료전지),마이크로유체역학,...
     * (연구실 소개)
     */
    public static String profile(Laboratory laboratory) {
        List<String> lines = new ArrayList<>();

        String header = laboratory.getLabName() + " (" + laboratory.getDepartment().getDepartmentName();
        if (laboratory.getProfessor() != null) {
            header += ", " + laboratory.getProfessor().getName() + " 교수";
        }
        lines.add(header + ")");

        if (hasText(laboratory.getResearchFieldRaw())) {
            lines.add("연구분야: " + laboratory.getResearchFieldRaw().strip());
        }
        if (hasText(laboratory.getIntroduction())) {
            lines.add(laboratory.getIntroduction().strip());
        }
        return String.join("\n", lines);
    }

    /**
     * 논문 원문: "제목. 게재처"
     */
    public static String publication(String title, String platform) {
        return hasText(platform) ? title.strip() + ". " + platform.strip() : title.strip();
    }

    // 원문이 바뀌었는지 비교하기 위한 SHA-256 (64자리 16진수)
    public static String hash(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
