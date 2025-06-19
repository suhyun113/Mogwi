package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/auth")
public class AuthController {

    @PersistenceContext
    private EntityManager entityManager;

    /** 회원가입 API
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> params) {
        String userid = params.get("userid");
        String userpass = params.get("userpass");
        String username = params.get("username");
        String usermail = params.get("usermail");
        String created_at = params.get("created_at");

        try {
            String checkSql = "SELECT * FROM users WHERE userid = ?";
            List<?> existing = entityManager.createNativeQuery(checkSql)
                    .setParameter(1, userid)
                    .getResultList();

            if (!existing.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "DUPLICATE"));
            }

            String insertSql = "INSERT INTO users(userid, userpass, username, usermail, created_at) VALUES (?, ?, ?, ?, ?)";
            entityManager.createNativeQuery(insertSql)
                    .setParameter(1, userid)
                    .setParameter(2, userpass)
                    .setParameter(3, username)
                    .setParameter(4, usermail)
                    .setParameter(5, created_at)
                    .executeUpdate();

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("회원가입 오류 발생: ", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR"));
        }
    }

    /** 로그인 API
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String userId = loginData.get("userid");
        String userPass = loginData.get("userpass");

        log.info("로그인 요청: {} / {}", userId, userPass);

        String sql = "SELECT * FROM users WHERE userid = ? AND userpass = ?";
        try {
            List<Object[]> result = entityManager.createNativeQuery(sql)
                    .setParameter(1, userId)
                    .setParameter(2, userPass)
                    .getResultList();

            if (!result.isEmpty()) {
                Object[] row = result.get(0);
                Map<String, Object> user = Map.of(
                        "userid", row[1],
                        "userpass", row[2],
                        "usermail", row[3],
                        "username", row[4],
                        "created_at", row[5]
                );
                return ResponseEntity.ok(Map.of("status", "OK", "user", user));
            } else {
                return ResponseEntity.ok(Map.of("status", "NOT"));
            }
        } catch (Exception e) {
            log.error("로그인 중 예외 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR"));
        }
    }
}
