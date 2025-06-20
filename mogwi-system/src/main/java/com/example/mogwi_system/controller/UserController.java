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

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/user") // 기본 경로 설정
public class UserController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * 사용자 정보 조회 API
     * GET /api/user/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserInfo(@PathVariable String userId) {
        String sql = "SELECT userid, userpass, usermail, username, created_at FROM users WHERE userid = ?";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userId);

        try {
            Object[] row = (Object[]) query.getSingleResult();
            Map<String, Object> user = new HashMap<>();
            user.put("userid", row[0]);
            user.put("userpass", row[1]);
            user.put("usermail", row[2]); // 이메일은 "usermail"이라는 키로 저장
            user.put("username", row[3]); // 닉네임은 "username"이라는 키로 저장
            user.put("created_at", row[4]);

            return ResponseEntity.ok(Map.of("status", "OK", "user", user));
        } catch (NoResultException e) {
            return ResponseEntity.status(404).body(Map.of("status", "NOT_FOUND", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "사용자 정보를 불러오는 데 실패했습니다."));
        }
    }

    /**
     * 사용자 정보 수정 (닉네임, 비밀번호)
     * PUT /api/user/{userId}/profile
     *
     * 요청 본문 예시:
     * {
     * "username": "새닉네임",
     * "currentPassword": "현재비밀번호",
     * "newPassword": "새비밀번호"
     * }
     */
    @PutMapping("/{userId}/profile")
    @Transactional // 데이터 변경 작업을 위해 트랜잭션 처리
    public ResponseEntity<?> updateUserProfile(@PathVariable String userId, @RequestBody Map<String, String> body) {
        String newNickname = body.get("username");
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        // 1. 닉네임 유효성 검사
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "닉네임은 필수 입력입니다."));
        }

        // 2. 현재 사용자 정보 조회 (특히 비밀번호 검증을 위해)
        String selectSql = "SELECT userpass FROM users WHERE userid = ?";
        Query selectQuery = entityManager.createNativeQuery(selectSql);
        selectQuery.setParameter(1, userId);

        String storedHashedPassword;
        try {
            storedHashedPassword = (String) selectQuery.getSingleResult();
        } catch (NoResultException e) {
            log.warn("사용자 ID를 찾을 수 없습니다: {}", userId);
            return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("사용자 비밀번호 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "데이터베이스 오류가 발생했습니다."));
        }

        // 3. 비밀번호 변경 로직 처리
        String updateSql = "UPDATE users SET username = ?";
        // 닉네임만 변경할 경우, 비밀번호 필드들은 무시합니다.
        // newPassword가 제공되었을 때만 비밀번호 변경 로직을 수행합니다.
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            // 현재 비밀번호 유효성 검사
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "비밀번호를 변경하려면 현재 비밀번호를 입력해야 합니다."));
            }
            if (!bCryptPasswordEncoder.matches(currentPassword, storedHashedPassword)) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "현재 비밀번호가 일치하지 않습니다."));
            }
            // 새 비밀번호 해싱
            String hashedNewPassword = bCryptPasswordEncoder.encode(newPassword);
            updateSql += ", userpass = ?"; // SQL에 비밀번호 업데이트 필드 추가
            log.info("사용자 {}의 비밀번호를 변경합니다.", userId);
        } else {
            // 새 비밀번호를 입력하지 않았는데, 현재 비밀번호나 새 비밀번호 확인 필드가 채워져 있다면 오류
            if ((currentPassword != null && !currentPassword.trim().isEmpty()) || (body.get("confirmNewPassword") != null && !body.get("confirmNewPassword").trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "새 비밀번호를 입력하지 않으려면, 현재 비밀번호 및 확인 필드를 비워두세요."));
            }
        }

        updateSql += " WHERE userid = ?"; // WHERE 절 추가

        Query updateQuery = entityManager.createNativeQuery(updateSql);
        int paramIndex = 1;

        updateQuery.setParameter(paramIndex++, newNickname); // 닉네임 파라미터 설정

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            updateQuery.setParameter(paramIndex++, bCryptPasswordEncoder.encode(newPassword)); // 해싱된 새 비밀번호 파라미터 설정
        }

        updateQuery.setParameter(paramIndex, userId); // userId 파라미터 설정

        try {
            int result = updateQuery.executeUpdate();

            if (result > 0) {
                log.info("사용자 {}의 프로필이 성공적으로 업데이트되었습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "OK", "message", "프로필 정보가 성공적으로 수정되었습니다."));
            } else {
                log.warn("사용자 {}의 프로필 업데이트 실패: 사용자를 찾을 수 없거나 변경 사항이 없습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "message", "프로필 업데이트에 실패했습니다. 사용자를 찾을 수 없거나 변경 사항이 없습니다."));
            }
        } catch (Exception e) {
            log.error("프로필 수정 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "프로필 업데이트 중 서버 오류가 발생했습니다."));
        }
    }
}
