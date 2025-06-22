package com.example.mogwi_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // UPLOAD_DIR_PATH를 public static final로 변경하여 외부에서 바로 접근 가능하게 하거나,
    // static getter를 통해 접근하게 합니다. 여기서는 static getter를 선호합니다.
    private static final String UPLOAD_DIR_PATH = "file:///E:/internetDB/images/"; // Windows 예시
    // Linux/macOS 경로: private static final String UPLOAD_DIR_PATH = "file:/home/user/mogwi_uploads/images/";
    // 또는 private static final String UPLOAD_DIR_PATH = "file:///var/lib/mogwi_uploads/images/";

    // UPLOAD_DIR_PATH의 물리적 경로를 반환하는 static 메서드 추가
    public static String getUploadDirPath() {
        // "file:///" 접두사를 제거하고 순수 물리적 경로만 반환
        return UPLOAD_DIR_PATH.replace("file:///", "").replace("file:/", "");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(UPLOAD_DIR_PATH);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://192.168.159.19:8080") // Vue.js 개발 서버 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}