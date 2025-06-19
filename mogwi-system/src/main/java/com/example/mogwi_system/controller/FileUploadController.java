package com.example.mogwi_system.controller; // 패키지명 확인

import com.example.mogwi_system.config.WebConfig; // WebConfig 임포트
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // UUID 임포트

@RestController
@RequestMapping("/api")
@Slf4j
public class FileUploadController {

    // WebConfig의 static 메서드를 사용하여 업로드 디렉토리의 물리적 경로를 가져옵니다.
    private final String uploadBaseDir = WebConfig.getUploadDirPath();

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("status", "FAIL");
            response.put("message", "업로드할 파일이 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 파일을 저장할 최종 물리적 경로를 구성합니다.
            // 예: C:/mogwi_uploads/images/
            Path uploadPath = Paths.get(uploadBaseDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // 디렉토리가 없으면 생성
                log.info("이미지 업로드 디렉토리 생성: {}", uploadPath.toString());
            }

            // 고유한 파일 이름 생성
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName); // 최종 파일 경로

            Files.copy(file.getInputStream(), filePath); // 파일 저장
            log.info("이미지 파일 저장 성공: {}", filePath.toString());

            // 클라이언트에서 접근할 수 있는 URL 반환
            // WebConfig의 addResourceHandlers에서 설정한 URL 패턴과 일치해야 합니다.
            // WebConfig가 "/images/**"를 매핑하므로, 여기서는 "/images/"로 시작해야 합니다.
            String imageUrl = "/images/" + uniqueFileName;

            response.put("status", "OK");
            response.put("imageUrl", imageUrl); // 클라이언트에 반환할 이미지 URL
            response.put("message", "이미지 업로드 성공");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            response.put("status", "FAIL");
            response.put("message", "이미지 업로드 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            response.put("status", "FAIL");
            response.put("message", "이미지 업로드 중 예상치 못한 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}