package com.lookuphere.stockguide.config;// ⚠️ 내 프로젝트의 실제 패키지 경로에 맞게 적어주세요.
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity // (SPR-SEC-M2-1) 보안 설정을 개발자가 직접 제어하겠다고 선언합니다.
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        // (SPR-SEC-M2-2) [순차] 최신 스프링 부트 3.x (시큐리티 6.x) 공식 람다식 규격을 적용하여 가림막을 걷어냅니다.
//        http
//                // 1. 리액트에서 날아오는 POST 주문을 가로막던 CSRF 검문소를 완전히 해제합니다.
//                .csrf(AbstractHttpConfigurer::disable)
//
//                // 2. 모든 웹 요청(/api/** 등)을 로그인 체크 없이 프리패스로 활짝 열어라!
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()
//                )
//
//                // 3. ⚠️ [핵심] 현재 에러의 주범이었던 구형 방식의 로그인 화면과 인증 창을 완전히 비활성화(disable) 시킵니다!
//                .formLogin(AbstractHttpConfigurer::disable)
//                .httpBasic(AbstractHttpConfigurer::disable);
//
//        return http.build(); // 안전하게 새 규격 자물쇠를 조립하여 스프링 엔진에 넘겨줍니다.
//    }
//}