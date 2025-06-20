package com.example.mogwi_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCryptPasswordEncoder 클래스 임포트
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 스프링 설정 클래스임을 명시
public class SecurityConfig {

    /**
     * BCryptPasswordEncoder 빈을 스프링 컨테이너에 등록합니다.
     * 이렇게 등록된 빈은 @Autowired를 통해 다른 컴포넌트에서 주입받아 사용할 수 있습니다.
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (개발 편의상, 프로덕션에서는 고려)
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll() // 모든 요청 허용 (임시)
                );
        return http.build();
    }
}