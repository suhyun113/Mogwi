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

        boolean isNicknameProvided = (newNickname != null && !newNickname.trim().isEmpty());
        boolean isPasswordChangeAttempt = (newPassword != null && !newPassword.trim().isEmpty());

        // 현재 사용자 정보 조회 (닉네임 비교 및 비밀번호 검증을 위해)
        String selectSql = "SELECT userpass, username FROM users WHERE userid = ?";
        Query selectQuery = entityManager.createNativeQuery(selectSql);
        selectQuery.setParameter(1, userId);

        String storedHashedPassword;
        String storedNickname; // 현재 저장된 닉네임을 가져옵니다.
        try {
            Object[] row = (Object[]) selectQuery.getSingleResult();
            storedHashedPassword = (String) row[0];
            storedNickname = (String) row[1]; // DB에 저장된 현재 닉네임
        } catch (NoResultException e) {
            log.warn("사용자 ID를 찾을 수 없습니다: {}", userId);
            return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "데이터베이스 오류가 발생했습니다."));
        }

        // 닉네임 변경 여부 확인
        boolean isNicknameChanged = isNicknameProvided && !newNickname.equals(storedNickname);
        // 새 비밀번호 시도 시도했지만 실제 비밀번호가 변경될지에 대한 여부는 아래 로직에서 결정됩니다.
        boolean isPasswordActuallyChanged = false;

        // 아무것도 변경할 내용이 없는 경우
        if (!isNicknameChanged && !isPasswordChangeAttempt) {
            return ResponseEntity.ok(Map.of("status", "NO_CHANGE", "message", "변경할 정보가 없습니다."));
        }


        StringBuilder updateSqlBuilder = new StringBuilder("UPDATE users SET ");
        Map<Integer, Object> params = new HashMap<>(); // 파라미터를 동적으로 관리
        int paramIndex = 1;

        if (isNicknameChanged) {
            updateSqlBuilder.append("username = ?");
            params.put(paramIndex++, newNickname);
            log.info("사용자 {}의 닉네임을 변경합니다. 새 닉네임: {}", userId, newNickname);
        }

        if (isPasswordChangeAttempt) {
            // 현재 비밀번호 유효성 검사
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "비밀번호를 변경하려면 현재 비밀번호를 입력해야 합니다."));
            }
            if (!bCryptPasswordEncoder.matches(currentPassword, storedHashedPassword)) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "현재 비밀번호가 일치하지 않습니다."));
            }

            // 새 비밀번호가 현재 비밀번호와 동일한지 확인 (새 비밀번호로 변경 시에만)
            if (bCryptPasswordEncoder.matches(newPassword, storedHashedPassword)) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "새 비밀번호가 현재 비밀번호와 동일합니다. 다른 비밀번호를 설정해주세요."));
            }

            // 새 비밀번호 해싱
            String hashedNewPassword = bCryptPasswordEncoder.encode(newPassword);
            if (isNicknameChanged) { // 닉네임도 변경하는 경우 콤마 추가
                updateSqlBuilder.append(", ");
            }
            updateSqlBuilder.append("userpass = ?");
            params.put(paramIndex++, hashedNewPassword);
            isPasswordActuallyChanged = true; // 비밀번호가 실제로 변경됨
            log.info("사용자 {}의 비밀번호를 변경합니다.", userId);
        } else {
            // 새 비밀번호를 입력하지 않았는데, 현재 비밀번호 필드가 채워져 있다면 오류
            // (confirmNewPassword는 백엔드에서 받지 않으므로 currentPassword만 체크)
            if (currentPassword != null && !currentPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "INVALID", "message", "새 비밀번호를 입력하지 않으려면, 현재 비밀번호 필드를 비워두세요."));
            }
        }

        // 최종 UPDATE 쿼리 생성
        updateSqlBuilder.append(" WHERE userid = ?");
        params.put(paramIndex, userId); // 마지막 파라미터는 userId

        // 변경할 내용이 정말 없는 경우 (닉네임 변경 시도했지만 값 동일, 비밀번호 변경 시도 안함)
        if (!isNicknameChanged && !isPasswordActuallyChanged) {
            return ResponseEntity.ok(Map.of("status", "NO_CHANGE", "message", "변경할 정보가 없습니다."));
        }

        Query updateQuery = entityManager.createNativeQuery(updateSqlBuilder.toString());
        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
            updateQuery.setParameter(entry.getKey(), entry.getValue());
        }

        try {
            int result = updateQuery.executeUpdate();

            if (result > 0) {
                String successMessage;
                if (isNicknameChanged && isPasswordActuallyChanged) {
                    successMessage = "프로필 정보(닉네임, 비밀번호)가 성공적으로 업데이트되었습니다.";
                } else if (isNicknameChanged) {
                    successMessage = "닉네임이 성공적으로 변경되었습니다.";
                } else if (isPasswordActuallyChanged) {
                    successMessage = "비밀번호가 성공적으로 변경되었습니다.";
                } else {
                    successMessage = "프로필 정보가 성공적으로 수정되었습니다."; // 예상치 못한 경우
                }
                log.info("사용자 {}의 프로필이 성공적으로 업데이트되었습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "OK", "message", successMessage));
            } else {
                // 이 else 블록은 대부분 유효한 userId를 찾았지만 아무것도 업데이트되지 않은 경우입니다.
                // 이는 이미 위에서 isNicknameChanged, isPasswordActuallyChanged 로 체크했지만 혹시 모를 경우를 대비합니다.
                log.warn("사용자 {}의 프로필 업데이트 실패: 변경 사항이 없거나 사용자를 찾을 수 없습니다.", userId);
                return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "message", "프로필 업데이트에 실패했습니다. 사용자를 찾을 수 없거나 변경 사항이 없습니다."));
            }
        } catch (Exception e) {
            log.error("프로필 수정 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", "프로필 업데이트 중 서버 오류가 발생했습니다."));
        }
    }
}
