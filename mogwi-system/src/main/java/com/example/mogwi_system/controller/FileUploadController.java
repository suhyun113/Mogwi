package com.example.mogwi_system.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    // TODO: 실제 환경에 맞게 이미지 저장 경로를 변경하세요.
    // 운영 환경에서는 이 경로를 외부 스토리지(AWS S3 등)로 변경하는 것을 권장합니다.
    private final String UPLOAD_DIR = "C:/mogwi_uploads/images/"; // Windows 예시
    // private final String UPLOAD_DIR = "/app/uploads/images/"; // Linux/Docker 예시

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("status", "FAIL");
            response.put("message", "업로드할 파일이 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 업로드 디렉토리 생성 (없으면)
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("이미지 업로드 디렉토리 생성: {}", UPLOAD_DIR);
            }

            // 고유한 파일명 생성 (겹치지 않도록 UUID 사용)
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath);
            log.info("이미지 파일 저장 성공: {}", filePath.toString());

            // 저장된 이미지에 접근할 수 있는 URL 생성
            // 이 URL은 Spring Boot의 정적 리소스 설정과 일치해야 합니다.
            // 예: "http://localhost:8000/images/" + newFileName
            String imageUrl = "/images/" + newFileName; // 서버의 정적 리소스 매핑 경로

            response.put("status", "OK");
            response.put("message", "이미지 업로드 성공");
            response.put("imageUrl", imageUrl); // 클라이언트에 URL 반환
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("이미지 저장 중 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "이미지 저장 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "이미지 업로드 중 예상치 못한 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}