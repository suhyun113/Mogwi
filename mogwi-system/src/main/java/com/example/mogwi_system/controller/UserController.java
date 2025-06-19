package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/user") // 사용자 관련 기본 경로
public class UserController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Helper method: 외부 사용자 ID(userid)로 내부 사용자 ID(id)를 조회합니다.
     * 모든 컨트롤러에서 공통으로 사용됩니다.
     *
     * @param userId 외부 사용자 ID (예: 로그인 ID)
     * @return 내부 사용자 ID (Primary Key)
     * @throws NoResultException 해당 사용자 ID를 찾을 수 없는 경우
     * @throws RuntimeException 데이터베이스 조회 중 오류 발생 시
     */
    private Long getInternalUserId(String userId) throws NoResultException {
        log.info("UserController: 외부 사용자 ID '{}'에 대한 내부 ID 조회 시도", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("UserController: 외부 사용자 ID '{}'에 해당하는 내부 사용자를 찾을 수 없음", userId);
            throw e;
        } catch (Exception e) {
            log.error("UserController: 외부 사용자 ID '{}'에 대한 내부 사용자 ID 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("내부 사용자 ID를 검색하는 데 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자 정보를 조회합니다. (예: 닉네임, 이메일)
     * GET /api/user/{userId}
     * Frontend (MypageView.vue)에서 사용자 닉네임을 불러올 때 사용됩니다.
     *
     * @param userId 조회할 사용자의 ID (users 테이블의 userid 필드)
     * @return 사용자 정보 (닉네임, 이메일 등)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId) {
        log.info("UserController - getUserProfile 호출됨: userId={}", userId);
        Map<String, Object> response = new HashMap<>();
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("UserController - getUserProfile: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID가 누락되었습니다."));
            }

            Long internalUserId;
            try {
                internalUserId = getInternalUserId(userId); // 먼저 내부 사용자 ID를 조회
            } catch (NoResultException e) {
                log.warn("UserController - getUserProfile: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
            }

            // usermail 컬럼을 조회하도록 SQL 쿼리 수정
            String sql = "SELECT username, usermail FROM users WHERE id = ?1";
            Object[] result;
            try {
                result = (Object[]) entityManager.createNativeQuery(sql)
                        .setParameter(1, internalUserId)
                        .getSingleResult();
            } catch (NoResultException e) {
                log.warn("UserController - getUserProfile: 내부 사용자 ID '{}'에 대한 프로필 정보를 찾을 수 없음. (데이터베이스에 해당 사용자 프로필 없음)", internalUserId);
                response.put("id", userId);
                response.put("nickname", null);
                response.put("email", null); // usermail이 없거나 null인 경우를 대비하여 null로 설정
                log.info("UserController - getUserProfile: 프로필 데이터가 없거나 불완전하여 기본값으로 응답 (userId: {})", userId);
                return ResponseEntity.ok(response);
            }

            // 조회된 결과에서 username과 usermail을 올바르게 매핑
            String nickname = (result != null && result.length > 0 && result[0] != null) ? result[0].toString() : null;
            String usermail = (result != null && result.length > 1 && result[1] != null) ? result[1].toString() : null;

            response.put("id", userId);
            response.put("nickname", nickname);
            response.put("email", usermail); // 프론트엔드는 'email'로 받으므로 여기서는 'email'로 매핑

            log.info("UserController - getUserProfile 성공: userId={}, nickname={}, email={}", userId, nickname, usermail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("UserController - getUserProfile: 사용자 프로필 조회 중 예상치 못한 오류 발생 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 사용자의 닉네임을 업데이트합니다.
     * PUT /api/user/{userId}/nickname
     *
     * @param userId 업데이트할 사용자의 ID (users 테이블의 userid 필드)
     * @param data   새로운 닉네임을 포함하는 맵 ({"nickname": "새닉네임"})
     * @return 성공/실패 메시지
     */
    @PutMapping("/{userId}/nickname")
    public ResponseEntity<Map<String, String>> updateNickname(
            @PathVariable String userId,
            @RequestBody Map<String, String> data) {
        log.info("UserController - updateNickname 호출됨: userId={}, data={}", userId, data);
        Map<String, String> response = new HashMap<>();
        String newNickname = data.get("nickname");

        if (userId == null || userId.trim().isEmpty() || newNickname == null || newNickname.trim().isEmpty()) {
            log.warn("UserController - updateNickname: 필수 입력값 누락. userId={}, newNickname={}", userId, newNickname);
            response.put("status", "ERROR");
            response.put("message", "사용자 ID 또는 새로운 닉네임이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("UserController - updateNickname: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("UserController - updateNickname: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // 중복 닉네임 확인 (현재 사용자 제외)
            String checkDuplicateSql = "SELECT COUNT(*) FROM users WHERE username = ?1 AND id != ?2";
            Long duplicateCount = ((Number) entityManager.createNativeQuery(checkDuplicateSql)
                    .setParameter(1, newNickname)
                    .setParameter(2, internalUserId)
                    .getSingleResult()).longValue();

            if (duplicateCount > 0) {
                log.warn("UserController - updateNickname: 닉네임 '{}'은 이미 사용 중입니다.", newNickname);
                response.put("status", "ERROR");
                response.put("message", "이미 사용 중인 닉네임입니다.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
            }

            // 닉네임 업데이트
            String updateSql = "UPDATE users SET username = ?1, updated_at = NOW() WHERE id = ?2";
            int updatedRows = entityManager.createNativeQuery(updateSql)
                    .setParameter(1, newNickname)
                    .setParameter(2, internalUserId)
                    .executeUpdate();

            if (updatedRows > 0) {
                log.info("UserController - updateNickname 성공: userId={}, 새 닉네임={}", userId, newNickname);
                response.put("status", "success");
                response.put("message", "닉네임이 성공적으로 변경되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                log.warn("UserController - updateNickname: 닉네임 업데이트 실패 - 사용자를 찾을 수 없거나 변경 사항 없음. userId={}", userId);
                response.put("status", "ERROR");
                response.put("message", "닉네임 업데이트에 실패했습니다. 사용자를 찾을 수 없거나 변경 사항이 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("UserController - updateNickname: 닉네임 업데이트 중 오류 발생 (internalUserId: {}, newNickname: {}): {}", internalUserId, newNickname, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
