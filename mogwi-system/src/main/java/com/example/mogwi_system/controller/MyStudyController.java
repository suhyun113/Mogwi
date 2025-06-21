package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/mystudy") // MyStudyController의 기본 경로 설정
public class MyStudyController {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper method: 외부 사용자 ID로 내부 사용자 ID를 조회
    private Long getInternalUserId(String userId) throws NoResultException {
        log.info("MyStudyController: 외부 사용자 ID '{}'에 대한 내부 ID 조회 시도", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("MyStudyController: 외부 사용자 ID '{}'에 해당하는 내부 사용자를 찾을 수 없음", userId);
            throw e;
        } catch (Exception e) {
            log.error("MyStudyController: 외부 사용자 ID '{}'에 대한 내부 사용자 ID 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("내부 사용자 ID를 검색하는 데 실패했습니다.", e);
        }
    }

    // Helper method: 문제의 카테고리 태그와 색상 코드 조회
    private List<Map<String, String>> getCategoriesForProblem(Long problemId) {
        List<Map<String, String>> categoriesWithColor = new ArrayList<>();
        try {
            String tagsSql = "SELECT c.tag_name, c.color_code FROM categories c " +
                    "JOIN problem_categories pc ON c.id = pc.category_id " +
                    "WHERE pc.problem_id = ?1";
            List<Object[]> tagsAndColors = entityManager.createNativeQuery(tagsSql)
                    .setParameter(1, problemId)
                    .getResultList();

            for (Object[] tagRow : tagsAndColors) {
                Map<String, String> categoryMap = new HashMap<>();
                categoryMap.put("tag_name", tagRow[0].toString());
                categoryMap.put("color_code", tagRow[1] != null ? tagRow[1].toString() : "#CCCCCC"); // null 처리 및 기본값 설정
                categoriesWithColor.add(categoryMap);
            }
        } catch (Exception e) {
            log.error("MyStudyController: 문제 ID {}에 대한 카테고리 조회 중 오류 발생: {}", problemId, e.getMessage(), e);
            // 오류 발생 시에도 빈 리스트 반환 (클라이언트에서 처리할 수 있도록)
        }
        return categoriesWithColor;
    }

    /**
     * 특정 사용자의 전체 학습 요약 통계를 조회합니다.
     * GET /api/mystudy/summary/{userId}
     * (예: 완벽, 모호, 망각 카운트)
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 전체 학습 요약 데이터
     */
    @GetMapping("/problem/{userId}")
    public ResponseEntity<Map<String, Object>> getOverallStudySummary(
            @PathVariable String userId) {
        log.info("MyStudyController - getOverallStudySummary 호출됨: userId={}", userId);
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("MyStudyController - getOverallStudySummary: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID가 누락되었습니다."));
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - getOverallStudySummary: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("MyStudyController - getOverallStudySummary: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            String sql = "SELECT " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' THEN 1 ELSE 0 END), 0) AS perfect_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' THEN 1 ELSE 0 END), 0) AS vague_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' THEN 1 ELSE 0 END), 0) AS forgotten_count, " +
                    "COALESCE(COUNT(ucs.id), 0) AS total_count " +
                    "FROM user_card_status ucs " +
                    "WHERE ucs.user_id = ?1";

            Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .getSingleResult();

            Map<String, Object> summary = new HashMap<>();
            summary.put("perfect", ((Number) result[0]).intValue());
            summary.put("vague", ((Number) result[1]).intValue());
            summary.put("forgotten", ((Number) result[2]).intValue());
            summary.put("total", ((Number) result[3]).intValue());

            log.info("MyStudyController - getOverallStudySummary 성공: userId={}, 요약: {}", userId, summary);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("MyStudyController - getOverallStudySummary: 학습 요약 데이터 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 특정 문제에 대한 학습 상태를 업데이트합니다.
     * PUT /api/mystudy/problems/{problemId}/status
     *
     * @param problemId 문제 ID
     * @param data      업데이트할 학습 상태 ('new', 'ongoing', 'completed') 및 사용자 ID
     * @return 성공/실패 메시지
     */
    @PutMapping("/problems/{problemId}/status")
    public ResponseEntity<Map<String, String>> updateProblemStatus(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - updateProblemStatus 호출됨: problemId={}, data={}", problemId, data);
        Map<String, String> response = new HashMap<>();
        String userId = (String) data.get("userId");
        String newStatus = (String) data.get("status");

        if (userId == null || newStatus == null || problemId == null) {
            log.warn("MyStudyController - updateProblemStatus: 필수 입력값 누락. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, status, problemId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - updateProblemStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // user_problem_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getSingleResult()).longValue();

            if (count > 0) {
                // 기존 레코드가 있으면 업데이트
                String updateSql = "UPDATE user_problem_status SET problem_status = ?1, updated_at = NOW() WHERE user_id = ?2 AND problem_id = ?3";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, newStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
                log.info("MyStudyController - updateProblemStatus: 문제 학습 상태 업데이트 성공. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "문제 학습 상태가 업데이트되었습니다.");
            } else {
                // 기존 레코드가 없으면 삽입 (problem_status와 updated_at만 설정)
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, problem_status, created_at, updated_at) VALUES (?1, ?2, ?3, NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newStatus)
                        .executeUpdate();
                log.info("MyStudyController - updateProblemStatus: 새 문제 학습 상태 삽입 성공. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "새 문제 학습 상태가 생성되었습니다.");
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MyStudyController - updateProblemStatus: 문제 학습 상태 업데이트/삽입 중 오류 발생 (problemId: {}, internalUserId: {}, newStatus: {}): {}", problemId, internalUserId, newStatus, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 특정 문제의 특정 카드에 대한 학습 상태를 업데이트합니다.
     * PUT /api/mystudy/cards/{cardId}/status
     *
     * @param cardId 카드 ID
     * @param data   업데이트할 카드 상태 ('perfect', 'vague', 'forgotten') 및 사용자 ID
     * @return 성공/실패 메시지
     */
    @PutMapping("/cards/{cardId}/status")
    public ResponseEntity<Map<String, String>> updateCardStatus(
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - updateCardStatus 호출됨: cardId={}, data={}", cardId, data);
        Map<String, String> response = new HashMap<>();
        String userId = (String) data.get("userId");
        String newStatus = (String) data.get("status");

        if (userId == null || newStatus == null || cardId == null) {
            log.warn("MyStudyController - updateCardStatus: 필수 입력값 누락. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, status, cardId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - updateCardStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateCardStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // 카드의 problem_id를 조회
            String getProblemIdSql = "SELECT problem_id FROM cards WHERE id = ?1";
            Long problemId = ((Number) entityManager.createNativeQuery(getProblemIdSql)
                    .setParameter(1, cardId)
                    .getSingleResult()).longValue();

            // user_card_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_card_status WHERE user_id = ?1 AND card_id = ?2 AND problem_id = ?3";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, cardId)
                    .setParameter(3, problemId)
                    .getSingleResult()).longValue();

            if (count > 0) {
                // 기존 레코드가 있으면 업데이트
                String updateSql = "UPDATE user_card_status SET card_status = ?1, updated_at = NOW() WHERE user_id = ?2 AND card_id = ?3 AND problem_id = ?4";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, newStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, cardId)
                        .setParameter(4, problemId)
                        .executeUpdate();
                log.info("MyStudyController - updateCardStatus: 카드 학습 상태 업데이트 성공. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "카드 학습 상태가 업데이트되었습니다.");
            } else {
                // 기존 레코드가 없으면 삽입
                String insertSql = "INSERT INTO user_card_status (user_id, problem_id, card_id, card_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, cardId)
                        .setParameter(4, newStatus)
                        .executeUpdate();
                log.info("MyStudyController - updateCardStatus: 새 카드 학습 상태 삽입 성공. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "새 카드 학습 상태가 생성되었습니다.");
            }
            return ResponseEntity.ok(response);

        } catch (NoResultException e) {
            log.warn("MyStudyController - updateCardStatus: 카드 ID '{}'에 해당하는 카드를 찾을 수 없음.", cardId);
            response.put("status", "ERROR");
            response.put("message", "카드를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateCardStatus: 카드 학습 상태 업데이트/삽입 중 오류 발생 (cardId: {}, internalUserId: {}, newStatus: {}): {}", cardId, internalUserId, newStatus, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 특정 문제에 대한 해당 사용자의 학습 상태만 삭제합니다.
     * user_card_status, user_problem_status만 삭제되며 문제나 카드, 좋아요/스크랩은 유지됩니다.
     * DELETE /api/mystudy/user-status/problems/{problemId}/user/{userId}
     */
    @DeleteMapping("/problem/{problemId}/user/{userId}")
    public ResponseEntity<Map<String, String>> deleteUserStudyStatusOnly(
            @PathVariable Long problemId,
            @PathVariable String userId) {

        Map<String, String> response = new HashMap<>();

        try {
            // user_card_status 삭제
            int deletedCardStatus = entityManager.createNativeQuery(
                            "DELETE FROM user_card_status WHERE problem_id = ?1 AND user_id = ?2")
                    .setParameter(1, problemId)
                    .setParameter(2, userId)
                    .executeUpdate();

            // user_problem_status 삭제
            int deletedProblemStatus = entityManager.createNativeQuery(
                            "DELETE FROM user_problem_status WHERE problem_id = ?1 AND user_id = ?2")
                    .setParameter(1, problemId)
                    .setParameter(2, userId)
                    .executeUpdate();

            response.put("status", "OK");
            response.put("message", "해당 사용자의 학습 상태가 성공적으로 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 학습 상태 삭제 중 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}