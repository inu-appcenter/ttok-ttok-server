package com.inuappcenter.team_2_project_server.global.config;

import com.inuappcenter.team_2_project_server.domain.member.service.JwtFilter;
import com.inuappcenter.team_2_project_server.domain.member.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * 모든 요청이 이 security 필터 체인을 거쳐감
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        // 기본 웹 보안
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // 세션/쿠키 기반 CSRF 보호 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // Spring 기본 로그인 페이지 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // BasicAuth 팝업/헤더 인증 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 서버 세션 생성 비활설화(JWT방식)


        // jwt 필터(api 요청 전에 호출되어야 함)
        httpSecurity
                .addFilterBefore(
                        new JwtFilter(jwtTokenProvider, userDetailsService, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);


        // api 인증
        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()

                        // 최우선 허용
                        .requestMatchers("/api/member/login", "/api/member/reissue").permitAll()

                        // 매핑 보류 연구과제 목록은 관리자 전용 (아래 /api/laboratory/** 공개 규칙보다 먼저 와야 함)
                        .requestMatchers(HttpMethod.GET, "/api/laboratory/research-projects/pending").hasRole("ADMIN")

                        // 인증이 필요없는 API
                        .requestMatchers(HttpMethod.GET, "/api/laboratory/**", "/api/college/**", "/api/research-metric/**", "/api/research-area-category/**").permitAll()

                        // 내 정보 조회는 로그인한 유저 본인이면 누구나 가능 (아래 관리자 전용 규칙보다 먼저 와야 함)
                        .requestMatchers(HttpMethod.GET, "/api/member/me").authenticated()

                        // 관리자 전용
                        .requestMatchers(HttpMethod.GET, "/api/member", "/api/member/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/laboratory/import", "/api/laboratory/research-projects/sync", "/api/chat/research-lab/index/sync", "/api/research-metric/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/research-metric/professor/*/author", "/api/laboratory/research-projects/*/laboratory").hasRole("ADMIN")

                        // 오류 제보: 생성(POST)은 로그인 유저 누구나, 조회/삭제는 관리자만
                        .requestMatchers(HttpMethod.GET, "/api/bug-report", "/api/bug-report/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/bug-report/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        return httpSecurity.build();
    }
}
