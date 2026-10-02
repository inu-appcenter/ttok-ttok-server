package com.inuappcenter.team_2_project_server.domain.researchMetric.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 한글 이름의 성씨가 영문 저자명(OpenAlex display_name)의 성과 일치하는지 판단한다.
 * 교수 ↔ OpenAlex 저자 매칭에서, 논문에 자주 등장하는 공저자(학생, 동료 교수)를 교수로 잘못 고르는 것을 막는 데 쓴다.
 *
 * - 영문명은 "Jung Ho Kim"(이름 먼저) / "Kim Jung Ho"(성 먼저) 둘 다 있어서 첫 토큰과 마지막 토큰을 모두 성 후보로 본다
 * - 표기는 실제 데이터에서 확인된 변형까지 포함한다 (예: 함 → Hahm, 박 → Bahk, 백 → Beck, 강 → Kahng)
 */
public final class KoreanSurnameMatcher {

    // 두 글자 성씨를 먼저 확인해야 "남궁"이 "남"으로 잘못 판단되지 않는다
    private static final Map<String, Set<String>> TWO_SYLLABLE_SURNAMES = Map.of(
            "황보", Set.of("hwangbo"),
            "남궁", Set.of("namgung", "namkung"),
            "선우", Set.of("sunwoo", "seonwoo"),
            "제갈", Set.of("jegal", "chegal"),
            "독고", Set.of("dokgo", "tokko"),
            "사공", Set.of("sagong"),
            "서문", Set.of("seomun"),
            "동방", Set.of("dongbang")
    );

    private static final Map<String, Set<String>> SURNAMES = Map.ofEntries(
            Map.entry("김", Set.of("kim", "gim", "ghim")),
            Map.entry("이", Set.of("lee", "yi", "rhee", "rhie", "ri", "li", "yee")),
            Map.entry("박", Set.of("park", "bak", "pak", "bahk", "pahk")),
            Map.entry("최", Set.of("choi", "choe", "choy")),
            Map.entry("정", Set.of("jung", "jeong", "chung", "jeung", "cheong", "joung", "jong", "chong")),
            Map.entry("강", Set.of("kang", "gang", "kahng")),
            Map.entry("조", Set.of("cho", "jo", "joe", "choh")),
            Map.entry("윤", Set.of("yoon", "yun", "youn")),
            Map.entry("장", Set.of("jang", "chang", "jhang")),
            Map.entry("임", Set.of("lim", "im", "rim", "yim", "leem")),
            Map.entry("한", Set.of("han", "hahn")),
            Map.entry("오", Set.of("oh", "o")),
            Map.entry("서", Set.of("seo", "suh", "so", "seoh")),
            Map.entry("신", Set.of("shin", "sin", "sheen")),
            Map.entry("권", Set.of("kwon", "gwon", "kweon")),
            Map.entry("황", Set.of("hwang", "whang")),
            Map.entry("안", Set.of("ahn", "an")),
            Map.entry("송", Set.of("song", "soung")),
            Map.entry("류", Set.of("ryu", "yoo", "yu", "ryoo", "lyu", "you")),
            Map.entry("유", Set.of("yoo", "yu", "ryu", "you")),
            Map.entry("전", Set.of("jeon", "chun", "jun", "chon")),
            Map.entry("홍", Set.of("hong")),
            Map.entry("고", Set.of("ko", "go", "koh")),
            Map.entry("문", Set.of("moon", "mun")),
            Map.entry("양", Set.of("yang")),
            Map.entry("손", Set.of("son", "sohn")),
            Map.entry("배", Set.of("bae", "pae", "bai")),
            Map.entry("백", Set.of("baek", "paik", "baik", "paek", "beck", "back", "bek")),
            Map.entry("허", Set.of("heo", "huh", "hur", "hu", "her")),
            Map.entry("남", Set.of("nam")),
            Map.entry("심", Set.of("shim", "sim")),
            Map.entry("노", Set.of("noh", "roh", "no", "ro", "rho")),
            Map.entry("하", Set.of("ha", "hah")),
            Map.entry("곽", Set.of("kwak", "gwak")),
            Map.entry("성", Set.of("sung", "seong")),
            Map.entry("차", Set.of("cha")),
            Map.entry("주", Set.of("joo", "ju", "chu", "choo")),
            Map.entry("우", Set.of("woo", "wu", "u")),
            Map.entry("구", Set.of("koo", "ku", "gu", "goo")),
            Map.entry("민", Set.of("min")),
            Map.entry("진", Set.of("jin", "chin")),
            Map.entry("나", Set.of("na", "ra", "rah")),
            Map.entry("지", Set.of("ji", "chi", "jee")),
            Map.entry("엄", Set.of("eom", "um", "uhm", "oum")),
            Map.entry("변", Set.of("byun", "byeon", "pyun")),
            Map.entry("채", Set.of("chae", "chai", "chea")),
            Map.entry("원", Set.of("won", "weon")),
            Map.entry("천", Set.of("chun", "cheon", "chon")),
            Map.entry("방", Set.of("bang", "pang")),
            Map.entry("공", Set.of("kong", "gong")),
            Map.entry("현", Set.of("hyun", "hyeon")),
            Map.entry("함", Set.of("ham", "hahm")),
            Map.entry("염", Set.of("yeom", "yum", "youm")),
            Map.entry("여", Set.of("yeo", "yuh", "yoe")),
            Map.entry("추", Set.of("choo", "chu")),
            Map.entry("도", Set.of("do", "doh", "toh")),
            Map.entry("소", Set.of("so", "soh")),
            Map.entry("석", Set.of("seok", "suk", "sok")),
            Map.entry("선", Set.of("sun", "seon")),
            Map.entry("설", Set.of("seol", "sul", "sol")),
            Map.entry("마", Set.of("ma")),
            Map.entry("길", Set.of("gil", "kil")),
            Map.entry("연", Set.of("yeon", "yun", "youn")),
            Map.entry("위", Set.of("wi", "wee", "we")),
            Map.entry("표", Set.of("pyo", "pyoe")),
            Map.entry("명", Set.of("myung", "myeong", "myoung")),
            Map.entry("기", Set.of("ki", "gi", "kee")),
            Map.entry("반", Set.of("ban", "pan")),
            Map.entry("왕", Set.of("wang")),
            Map.entry("금", Set.of("keum", "kum", "geum")),
            Map.entry("옥", Set.of("ok", "ock")),
            Map.entry("육", Set.of("yook", "yuk", "yug")),
            Map.entry("인", Set.of("in", "yin")),
            Map.entry("맹", Set.of("maeng")),
            Map.entry("제", Set.of("je", "jae")),
            Map.entry("모", Set.of("mo", "moh")),
            Map.entry("탁", Set.of("tak", "tahk")),
            Map.entry("국", Set.of("kook", "guk", "kuk")),
            Map.entry("어", Set.of("eo", "uh")),
            Map.entry("은", Set.of("eun", "un")),
            Map.entry("편", Set.of("pyun", "pyeon")),
            Map.entry("용", Set.of("yong")),
            Map.entry("예", Set.of("ye", "yeh")),
            Map.entry("경", Set.of("kyung", "gyeong", "kyeong")),
            Map.entry("봉", Set.of("bong", "pong")),
            Map.entry("사", Set.of("sa")),
            Map.entry("부", Set.of("boo", "bu", "pu")),
            Map.entry("단", Set.of("dan")),
            Map.entry("시", Set.of("si", "shi")),
            Map.entry("피", Set.of("pi", "pee")),
            Map.entry("감", Set.of("kam", "gam")),
            Map.entry("라", Set.of("ra", "la", "na")),
            Map.entry("빈", Set.of("bin")),
            Map.entry("범", Set.of("bum", "beom")),
            Map.entry("승", Set.of("seung", "sung")),
            Map.entry("태", Set.of("tae")),
            Map.entry("계", Set.of("kye", "gye")),
            Map.entry("형", Set.of("hyung", "hyeong")),
            Map.entry("복", Set.of("bok")),
            Map.entry("호", Set.of("ho")),
            Map.entry("견", Set.of("kyun", "gyeon"))
    );

    private KoreanSurnameMatcher() {
    }

    /**
     * @return 일치하면 true, 불일치하면 false, 판단할 수 없으면(한글 이름이 아니거나 목록에 없는 성씨) empty
     */
    public static Optional<Boolean> matches(String koreanName, String englishName) {
        if (koreanName == null || englishName == null) {
            return Optional.empty();
        }
        String name = koreanName.trim();
        if (name.isEmpty() || !isHangul(name.charAt(0))) {
            return Optional.empty();
        }

        // OpenAlex 저자명이 한글로 들어 있는 경우도 있다 (예: "민서영")
        String displayName = englishName.trim();
        if (!displayName.isEmpty() && isHangul(displayName.charAt(0))) {
            return Optional.of(displayName.startsWith(surnameOf(name)));
        }

        Set<String> romanizations = romanizationsOf(name);
        if (romanizations == null) {
            return Optional.empty();
        }

        List<String> tokens = tokenize(displayName);
        if (tokens.isEmpty()) {
            return Optional.of(false);
        }

        // "Jung Ho Kim" / "Kim Jung Ho" 양쪽 모두 허용, 두 글자 성씨는 "Hwang-Bo"처럼 나뉜 경우까지 고려
        String first = tokens.get(0);
        String last = tokens.get(tokens.size() - 1);
        String firstTwo = tokens.size() > 1 ? first + tokens.get(1) : first;
        String lastTwo = tokens.size() > 1 ? tokens.get(tokens.size() - 2) + last : last;

        return Optional.of(romanizations.contains(first) || romanizations.contains(last)
                || romanizations.contains(firstTwo) || romanizations.contains(lastTwo));
    }

    // 성씨가 목록에 있는지 (매칭 시 성씨 필터를 쓸 수 있는 교수인지 판단)
    public static boolean isKnownSurname(String koreanName) {
        return koreanName != null && !koreanName.isBlank() && romanizationsOf(koreanName.trim()) != null;
    }

    private static Set<String> romanizationsOf(String name) {
        if (name.length() >= 3 && TWO_SYLLABLE_SURNAMES.containsKey(name.substring(0, 2))) {
            return TWO_SYLLABLE_SURNAMES.get(name.substring(0, 2));
        }
        return SURNAMES.get(name.substring(0, 1));
    }

    private static String surnameOf(String name) {
        if (name.length() >= 3 && TWO_SYLLABLE_SURNAMES.containsKey(name.substring(0, 2))) {
            return name.substring(0, 2);
        }
        return name.substring(0, 1);
    }

    // 하이픈(-, ‐), 마침표, 쉼표, 보이지 않는 서식 문자(예: U+202C)를 제거하고 소문자 토큰으로 나눈다
    private static List<String> tokenize(String displayName) {
        String normalized = displayName.toLowerCase(Locale.ROOT)
                .replaceAll("\\p{Cf}", "")
                .replaceAll("[\\-‐‑.,]", " ")
                .trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(normalized.split("\\s+"));
    }

    private static boolean isHangul(char c) {
        return c >= '가' && c <= '힣';
    }
}
