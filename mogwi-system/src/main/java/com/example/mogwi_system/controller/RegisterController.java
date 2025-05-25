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

@RestController
@Slf4j
@Transactional
public class RegisterController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, VerificationInfo> verificationMap = new HashMap<>();

    // 이메일 인증코드 전송
    @PostMapping("/api/send-email-code")
    public ResponseEntity<?> sendEmailCode(@RequestBody Map<String, String> body) {
        String usermail = body.get("usermail");

        // 등록된 이메일 검사
        String checkSql = "SELECT * FROM users WHERE usermail = ?";
        Query checkQuery = entityManager.createNativeQuery(checkSql);
        checkQuery.setParameter(1, usermail);

        List<?> existing = checkQuery.getResultList();
        if (!existing.isEmpty()) {
        return ResponseEntity.ok(Map.of("status", "DUPLICATE", "message", "이미 가입된 계정입니다."));
        }

        // 인증코드 : 숫자 + 대문자 영어 6자리 생성
        String code = generateCode();
        LocalDateTime expire = LocalDateTime.now().plusMinutes(3);

        verificationMap.put(usermail, new VerificationInfo(code, expire));

        // 이메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(usermail);
            message.setSubject("[모귀] 회원가입 인증코드");
            message.setText("인증코드는 다음과 같습니다:\n\n" + code + "\n\n3분 내에 입력해주세요.");
            mailSender.send(message);

            log.info("인증코드 발송 완료 - {} : {}", usermail, code);
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("인증코드 전송 실패)", e);
            System.out.println("ENV USER: " + System.getenv("SPRING_MAIL_USERMAIL"));

            return ResponseEntity.ok(Map.of("status", "FAIL", "message", "인증코드 전송 실패"));
        }
    }

    // 인증 코드 생성
    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
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

    // 인증코드 확인
    @PostMapping("/api/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@RequestBody Map<String, String> body) {
        String usermail = body.get("usermail");
        String inputCode = body.get("code");

        VerificationInfo info = verificationMap.get(usermail);
        // 정보 비어있는 경우 또는 타이머 오버
        if (info == null || LocalDateTime.now().isAfter(info.expire)) {
            return ResponseEntity.ok(Map.of("status", "EXPIRED"));
        }

        // 사용자가 입력한 코드가 동일한 경우
        if (info.code.equals(inputCode)) {
            return ResponseEntity.ok(Map.of("status", "OK"));
        } else {
            return ResponseEntity.ok(Map.of("status", "FAIL"));
        }
    }

    // 회원가입
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> params) {
        String userid = params.get("userid");
        String userpass = params.get("userpass");
        String username = params.get("username");
        String usermail = params.get("usermail");
        String created_at = params.get("created_at");

        try {
            String checkSql = "SELECT * FROM users WHERE userid = ?";
            Query checkQuery = entityManager.createNativeQuery(checkSql);
            checkQuery.setParameter(1, userid);

            List<Object[]> existing = checkQuery.getResultList();
            if (!existing.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "DUPLICATE"));
            }

            String insertSql = "INSERT INTO users(userid, userpass, username, usermail, created_at) VALUES (?, ?, ?, ?, ?)";
            Query insertQuery = entityManager.createNativeQuery(insertSql);
            insertQuery.setParameter(1, userid);
            insertQuery.setParameter(2, userpass);
            insertQuery.setParameter(3, username);
            insertQuery.setParameter(4, usermail);
            insertQuery.setParameter(5, created_at);

            int result = insertQuery.executeUpdate();
            if (result > 0) {
                return ResponseEntity.ok(Map.of("status", "OK"));
            } else {
                return ResponseEntity.ok(Map.of("status", "FAIL"));
            }
        } catch (Exception e) {
            log.error("회원가입 오류 발생: ", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR"));
        }
    }
}

