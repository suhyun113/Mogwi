package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/auth")
public class AuthController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    /** 회원가입 API
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> params) {
        String userid = params.get("userid");
        String userpass = params.get("userpass"); // 평문 비밀번호
        String username = params.get("username");
        String usermail = params.get("usermail");

        String created_at = params.getOrDefault("created_at",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "INVALID_NICKNAME", "message", "닉네임은 필수 입력입니다."));
        }

        if (userpass == null || userpass.length() < 6) { // 최소 6자리로 가정
            return ResponseEntity.badRequest().body(Map.of("status", "INVALID_PASSWORD", "message", "비밀번호는 최소 6자 이상이어야 합니다."));
        }

        try {
            String checkIdSql = "SELECT userid FROM users WHERE userid = ?";
            List<?> existingIds = entityManager.createNativeQuery(checkIdSql)
                    .setParameter(1, userid)
                    .getResultList();

            if (!existingIds.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "DUPLICATE_ID", "message", "이미 존재하는 아이디입니다."));
            }

            String checkEmailSql = "SELECT usermail FROM users WHERE usermail = ?";
            List<?> existingEmails = entityManager.createNativeQuery(checkEmailSql)
                    .setParameter(1, usermail)
                    .getResultList();

            if (!existingEmails.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "DUPLICATE_EMAIL", "message", "이미 등록된 이메일입니다."));
            }

            String hashedPassword = bCryptPasswordEncoder.encode(userpass);

            String insertSql = "INSERT INTO users(userid, userpass, username, usermail, created_at) VALUES (?, ?, ?, ?, ?)";
            entityManager.createNativeQuery(insertSql)
                    .setParameter(1, userid)
                    .setParameter(2, hashedPassword)
                    .setParameter(3, username)
                    .setParameter(4, usermail)
                    .setParameter(5, created_at)
                    .executeUpdate();

            log.info("회원가입 성공: {}", userid);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "회원가입이 성공적으로 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원가입 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "회원가입 중 서버 오류가 발생했습니다."));
        }
    }

    /** 로그인 API
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String userId = loginData.get("userid");
        String userPass = loginData.get("userpass");

        log.info("로그인 요청: {}", userId);

        String sql = "SELECT userid, userpass, usermail, username, created_at FROM users WHERE userid = ?";
        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, userId);

            Object[] row;
            try {
                row = (Object[]) query.getSingleResult();
            } catch (NoResultException e) {
                log.warn("로그인 실패: 사용자 ID '{}'를 찾을 수 없습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "NOT", "message", "아이디 또는 비밀번호가 일치하지 않습니다."));
            }

            String storedHashedPassword = (String) row[1];

            if (bCryptPasswordEncoder.matches(userPass, storedHashedPassword)) {
                log.info("로그인 성공: {}", userId);
                Map<String, Object> user = new HashMap<>();
                user.put("userid", row[0]);
                user.put("userpass", null); // 보안상 비밀번호 정보는 클라이언트에 노출하지 않습니다.
                user.put("usermail", row[2]);
                user.put("username", row[3]);
                user.put("created_at", row[4]);

                return ResponseEntity.ok(Map.of("status", "OK", "user", user, "message", "로그인 성공"));
            } else {
                log.warn("로그인 실패: 사용자 ID '{}'의 비밀번호가 일치하지 않습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "NOT", "message", "아이디 또는 비밀번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            log.error("로그인 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "로그인 처리 중 서버 오류가 발생했습니다."));
        }
    }
}