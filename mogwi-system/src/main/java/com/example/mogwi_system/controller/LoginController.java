package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // REST API 컨트롤러임을 표시
@Slf4j // log 쉽게 사용하는 어노테이션
public class LoginController {

    @PersistenceContext // 어노테이션 EntityManager를 사용하기 쉽도록
    private EntityManager entityManager; // EntityManager 주입

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String userId = loginData.get("userid");
        String userPass = loginData.get("userpass");

        log.info("요청된 로그인 정보: " + userId + " " + userPass);
        String sql = "SELECT * FROM users WHERE userid = ? AND userpass = ?";

        try {
            List<Object[]> result = entityManager.createNativeQuery(sql)
                    .setParameter(1, userId)
                    .setParameter(2, userPass)
                    .getResultList();

            if (!result.isEmpty()) {
                log.info("로그인 성공");
                Object[] row = result.get(0);

                Map<String, Object> user = new HashMap<>();
                user.put("userid", row[1]);
                user.put("userpass", row[2]);
                user.put("usermail", row[3]);
                user.put("username", row[4]);
                user.put("created_at", row[5]);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "OK");
                response.put("user", user);

                return ResponseEntity.ok(response);
            } else {
                log.info("로그인 실패 - 아이디 또는 비밀번호 불일치");
                Map<String, Object> fail = new HashMap<>();
                fail.put("status", "NOT");

                return ResponseEntity.ok(fail);
            }
        } catch (Exception e) {
            log.error("로그인 중 예외 발생: {}", e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");

            return ResponseEntity.status(500).body(err);
        }
    }
}
