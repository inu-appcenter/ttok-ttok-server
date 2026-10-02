package com.inuappcenter.team_2_project_server.domain.department.enums;

import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Department {

    // 인문대학
    KOREAN("국어국문학과", College.COLLEGE_OF_HUMANITIES),
    ENGLISH("영어영문학과", College.COLLEGE_OF_HUMANITIES),
    GERMAN("독어독문학과", College.COLLEGE_OF_HUMANITIES),
    FRENCH("불어불문학과", College.COLLEGE_OF_HUMANITIES),
    JAPANESE("일본지역문화학과", College.COLLEGE_OF_HUMANITIES),
    CHINESE("중어중국학과", College.COLLEGE_OF_HUMANITIES),

    // 자연과학대학
    MATHEMATICS("수학과", College.COLLEGE_OF_NATURAL_SCIENCES),
    PHYSICS("물리학과", College.COLLEGE_OF_NATURAL_SCIENCES),
    CHEMISTRY("화학과", College.COLLEGE_OF_NATURAL_SCIENCES),
    FASHION("패션산업학과", College.COLLEGE_OF_NATURAL_SCIENCES),
    MARINE("해양학과", College.COLLEGE_OF_NATURAL_SCIENCES),

    // 사회과학대학
    SOCIAL_WELFARE("사회복지학과", College.COLLEGE_OF_SOCIAL_SCIENCES),
    MEDIA_COMMUNICATION("미디어커뮤니케이션학과", College.COLLEGE_OF_SOCIAL_SCIENCES),
    LIBRARY_INFO("문헌정보학과", College.COLLEGE_OF_SOCIAL_SCIENCES),
    CREATIVE_HRD("창의인재개발학과", College.COLLEGE_OF_SOCIAL_SCIENCES),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION("행정학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE),
    POLITICS_DIPLOMACY("정치외교학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE),
    ECONOMICS("경제학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE),
    TRADE("Global Trade & Service 학부(무역학부)", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE),
    CONSUMER_SCIENCE("소비자학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE),

    // 공과대학
    ENERGY_CHEMICAL("에너지화학공학과", College.COLLEGE_OF_ENGINEERING),
    ELECTRICAL_ENGINEERING("전기공학과", College.COLLEGE_OF_ENGINEERING),
    ELECTRONICS_ENGINEERING("전자공학부", College.COLLEGE_OF_ENGINEERING),
    INDUSTRIAL_MANAGEMENT("산업경영공학과", College.COLLEGE_OF_ENGINEERING),
    MATERIAL_SCIENCE("신소재공학과", College.COLLEGE_OF_ENGINEERING),
    MECHANICAL_ENGINEERING("기계공학과", College.COLLEGE_OF_ENGINEERING),
    BIO_ROBOTICS_ENGINEERING("바이오-로봇시스템공학과", College.COLLEGE_OF_ENGINEERING),
    SAFETY_ENGINEERING("안전공학과", College.COLLEGE_OF_ENGINEERING),

    // 정보기술대학
    COMPUTER_ENGINEERING("컴퓨터공학부", College.COLLEGE_OF_INFORMATION_TECHNOLOGY),
    INFORMATION_COMMUNICATION_ENGINEERING("정보통신공학과", College.COLLEGE_OF_INFORMATION_TECHNOLOGY),
    EMBEDDED_SYSTEM("임베디드시스템공학과", College.COLLEGE_OF_INFORMATION_TECHNOLOGY),

    // 경영대학
    BUSINESS_ADMINISTRATION("경영학부", College.COLLEGE_OF_BUSINESS_ADMINISTRATION),
    DATA_SCIENCE("데이터과학과", College.COLLEGE_OF_BUSINESS_ADMINISTRATION),
    TAX_ACCOUNTING("세무회계학과", College.COLLEGE_OF_BUSINESS_ADMINISTRATION),

    // 예술체육대학
    FINE_ARTS_KOREAN_PAINTING("조형예술학부(한국화전공)", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),
    FINE_ARTS_WESTERN_PAINTING("조형예술학부(서양화전공)", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),
    DESIGN("디자인학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),
    PERFORMING_ART("공연예술학과", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),
    SPORTS_SCIENCE("스포츠과학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),
    HEALTH_EXERCISE("운동건강학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION),

    // 사범대학
    KOREAN_EDUCATION("국어교육과", College.COLLEGE_OF_EDUCATION),
    ENGLISH_EDUCATION("영어교육과", College.COLLEGE_OF_EDUCATION),
    JAPANESE_EDUCATION("일어교육과", College.COLLEGE_OF_EDUCATION),
    MATH_EDUCATION("수학교육과", College.COLLEGE_OF_EDUCATION),
    PHYSICAL_EDUCATION("체육교육과", College.COLLEGE_OF_EDUCATION),
    EARLY_CHILDHOOD_EDUCATION("유아교육과", College.COLLEGE_OF_EDUCATION),
    HISTORY_EDUCATION("역사교육과", College.COLLEGE_OF_EDUCATION),
    ETHICS_EDUCATION("윤리교육과", College.COLLEGE_OF_EDUCATION),

    // 도시과학대학
    URBAN_ADMINISTRATION("도시행정학과", College.COLLEGE_OF_URBAN_SCIENCE),
    CIVIL_ENVIRONMENT_ENGINEERING("도시환경공학부(건설환경공학전공)", College.COLLEGE_OF_URBAN_SCIENCE),
    ENVIRONMENT_ENGINEERING("도시환경공학부(환경공학전공)", College.COLLEGE_OF_URBAN_SCIENCE),
    URBAN_ENGINEERING("도시공학과", College.COLLEGE_OF_URBAN_SCIENCE),
    URBAN_ARCHITECTURE_ENGINEERING("도시건축학부(건축공학전공)", College.COLLEGE_OF_URBAN_SCIENCE),
    URBAN_ARCHITECTURE_ARCHITECTURE("도시건축학부(도시건축학전공)", College.COLLEGE_OF_URBAN_SCIENCE),

    // 생명과학기술대학
    LIFE_SCIENCE("생명과학부(생명과학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY),
    LIFE_SCIENCE_MOLECULAR("생명과학부(분자의생명전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY),
    BIOENGINEERING("생명공학부(생명공학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY),
    BIOENGINEERING_NANO("생명공학부(나노바이오공학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY),

    // 융합자유전공대학
    LIBERAL_ARTS("자유전공학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES),
    INTERNATIONAL_LIBERAL_ARTS("국제자유전공학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES),
    CONVERGENCE("융합학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES),

    // 동북아국제통상물류학부
    NORTHEAST_ASIAN_TRADE("동북아국제통상전공", College.COLLEGE_OF_NULL),
    SMART_LOGISTICS_ENGINEERING("스마트물류공학전공", College.COLLEGE_OF_NULL),
    IBE("IBE전공", College.COLLEGE_OF_NULL),

    // 법학부
    LAW("법학부", College.COLLEGE_OF_NULL),

    ;

    private final String departmentName;    // 학과명
    private final College collegeName;      // 단과대명

    public static Department fromDepartmentName(String parsedDepartmentName) {
        return Arrays.stream(values())
                .filter(college -> college.departmentName.equals(parsedDepartmentName))
                .findFirst()
                .orElseThrow(() -> new MyException(ErrorCode.INVALID_INPUT));
    }
}
