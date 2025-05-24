package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@Slf4j
@Transactional
public class RegisterController {

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, VerificationInfo> verificationMap = new HashMap<>();

    // 이메일 인증코드 전송
    @PostMapping("/api/send-email-code")
    public ResponseEntity<?> sendEmailCode(@RequestBody Map<String, String> body) {
        String usermail = body.get("usermail");
        String code = String.format("%06d", new Random().nextInt(1000000));
        LocalDateTime expire = LocalDateTime.now().plusMinutes(3);

        verificationMap.put(usermail, new VerificationInfo(code, expire));

        log.info("[이메일 인증코드 발송] 이메일: {}, 코드: {}", usermail, code);

        return ResponseEntity.ok(Map.of("status", "OK", "code", code)); // ⚠️ 프로덕션에서는 code 응답 제거
    }

    // ⚙️ 내부 인증정보 클래스
    static class VerificationInfo {
        String code;
        LocalDateTime expire;

        VerificationInfo(String code, LocalDateTime expire) {
            this.code = code;
            this.expire = expire;
        }
    }
}

