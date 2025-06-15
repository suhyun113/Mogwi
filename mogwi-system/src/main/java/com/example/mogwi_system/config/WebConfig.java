package com.example.mogwi_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // TODO: FileUploadController의 UPLOAD_DIR과 일치하도록 경로를 변경하세요.
    // 윈도우 경로: "file:///C:/mogwi_uploads/images/" (슬래시 3개)
    // Linux/macOS 경로: "file:/home/user/mogwi_uploads/images/" 또는 "file:///var/lib/mogwi_uploads/images/"
    private final String UPLOAD_DIR_PATH = "file:///C:/mogwi_uploads/images/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // '/images/**' URL 패턴으로 들어오는 요청을 UPLOAD_DIR_PATH (물리적 파일 경로)에서 찾도록 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations(UPLOAD_DIR_PATH);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 엔드포인트에 CORS 적용
                .allowedOrigins("http://localhost:8080", "http://127.0.0.1:8080") // Vue.js 개발 서버의 오리진 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 자격 증명(쿠키, 인증 헤더 등) 허용
                .maxAge(3600); // Preflight 요청 캐싱 시간 (초)
    }
}