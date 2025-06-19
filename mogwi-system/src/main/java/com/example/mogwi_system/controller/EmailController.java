package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 이메일 인증 관련 API 컨트롤러
 */
@RestController
@Slf4j
@Transactional
public class EmailController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, VerificationInfo> verificationMap = new HashMap<>();

    /** 이메일 인증코드 전송 API
     *  POST /api/send-email-code
     */
    @PostMapping("/api/send-email-code")
    public ResponseEntity<?> sendEmailCode(@RequestBody Map<String, String> body) {
        String usermail = body.get("usermail");

        String checkSql = "SELECT * FROM users WHERE usermail = ?";
        Query checkQuery = entityManager.createNativeQuery(checkSql);
        checkQuery.setParameter(1, usermail);

        List<?> existing = checkQuery.getResultList();
        if (!existing.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "DUPLICATE", "message", "이미 가입된 계정입니다."));
        }

        String code = generateCode();
        LocalDateTime expire = LocalDateTime.now().plusMinutes(3);
        verificationMap.put(usermail, new VerificationInfo(code, expire));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(usermail);
            message.setSubject("[모귀] 회원가입 인증코드");
            message.setText("인증코드는 다음과 같습니다:\n\n" + code + "\n\n3분 내에 입력해주세요.");
            mailSender.send(message);

            log.info("인증코드 발송 완료 - {} : {}", usermail, code);
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("인증코드 전송 실패", e);
            return ResponseEntity.ok(Map.of("status", "FAIL", "message", "인증코드 전송 실패"));
        }
    }

    /** 이메일 인증코드 검증 API
     *  POST /api/verify-email-code
     */
    @PostMapping("/api/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@RequestBody Map<String, String> body) {
        String usermail = body.get("usermail");
        String inputCode = body.get("code");

        VerificationInfo info = verificationMap.get(usermail);

        if (info == null || LocalDateTime.now().isAfter(info.expire)) {
            return ResponseEntity.ok(Map.of("status", "EXPIRED"));
        }

        if (info.code.equals(inputCode)) {
            return ResponseEntity.ok(Map.of("status", "OK"));
        } else {
            return ResponseEntity.ok(Map.of("status", "FAIL"));
        }
    }

    // 인증코드 생성
    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 인증 정보 저장용 내부 클래스
    static class VerificationInfo {
        String code;
        LocalDateTime expire;

        VerificationInfo(String code, LocalDateTime expire) {
            this.code = code;
            this.expire = expire;
        }
    }
}
