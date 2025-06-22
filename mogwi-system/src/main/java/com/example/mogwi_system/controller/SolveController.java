// src/main/java/com/example/mogwi_system/controller/SolveController.java

package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api")
public class SolveController {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper method to get internal user ID from userid
    private Long getInternalUserId(String userId) throws NoResultException {
        log.info("Attempting to get internal ID for external userId: {}", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("No internal user found for external userId: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting internal user ID for external userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve internal user ID", e);
        }
    }

    /**
     * 특정 문제(problemId)에 속한 카드 목록과 해당 사용자의 학습 상태를 조회합니다.
     * GET /api/study/{problemId}/solve
     *
     * @param problemId 조회할 문제의 ID
     * @param currentUserId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 문제 카드 목록과 각 카드의 학습 상태를 포함하는 응답
     */
    @GetMapping("/study/{problemId}/solve")
    public ResponseEntity<List<Map<String, Object>>> getCardsForProblem(
            @PathVariable Long problemId,
            @RequestParam(required = false) String currentUserId
    ) {
        try {
            String effectiveCurrentUserId = (currentUserId != null) ? currentUserId : "";

            Long internalUserId = null;
            if (!effectiveCurrentUserId.isEmpty()) {
                List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                        .setParameter(1, effectiveCurrentUserId)
                        .getResultList();
                if (!userResult.isEmpty()) {
                    internalUserId = ((Number) userResult.get(0)).longValue();
                }
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT c.id, c.question, c.correct, c.image_url, " +
                            "IFNULL(ucs.card_status, 'new') AS card_status " +
                            "FROM cards c " +
                            "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND c.problem_id = ucs.problem_id ");

            if (internalUserId != null) {
                sql.append("AND ucs.user_id = ?2 ");
            }

            sql.append("WHERE c.problem_id = ?1 " +
                    "ORDER BY c.id ASC");

            var queryObj = entityManager.createNativeQuery(sql.toString());
            queryObj.setParameter(1, problemId);
            if (internalUserId != null) {
                queryObj.setParameter(2, internalUserId);
            }

            List<Object[]> results = queryObj.getResultList();
            List<Map<String, Object>> cards = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("id", ((Number) row[0]).longValue());
                card.put("question", row[1]);
                card.put("correct", row[2]);
                card.put("imageUrl", row[3]);
                card.put("cardStatus", row[4].toString());
                cards.add(card);
            }

            if (cards.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }

            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("문제 카드 및 학습 상태 조회 중 오류 발생 (SolveController): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 특정 카드의 사용자 학습 상태를 업데이트합니다.
     * POST /api/solve/{cardId}/status
     *
     * @param cardId 상태를 업데이트할 카드의 ID
     * @param data 사용자 ID (userId), 새로운 카드 상태 (cardStatus), 문제 ID (problemId)를 포함하는 맵
     * @return 처리 결과 상태 (OK 또는 ERROR)
     */
    @PostMapping("/solve/{cardId}/status")
    public ResponseEntity<Map<String, Object>> updateCardStatus(
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> data) {

        String userId = (String) data.get("userId");
        String cardStatus = (String) data.get("cardStatus");
        Long problemId = ((Number) data.get("problemId")).longValue();

        if (userId == null || cardStatus == null || cardId == null || problemId == null ||
                (!cardStatus.equals("perfect") && !cardStatus.equals("vague") && !cardStatus.equals("forgotten"))) {
            log.warn("updateCardStatus: 유효하지 않은 입력값입니다. userId: {}, cardStatus: {}, cardId: {}, problemId: {}", userId, cardStatus, cardId, problemId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "유효하지 않은 카드 상태 또는 입력값입니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("updateCardStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("updateCardStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            List<?> existingCardStatus = entityManager.createNativeQuery(
                            "SELECT id FROM user_card_status WHERE user_id = ?1 AND card_id = ?2 AND problem_id = ?3")
                    .setParameter(1, internalUserId)
                    .setParameter(2, cardId)
                    .setParameter(3, problemId)
                    .getResultList();

            if (existingCardStatus.isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO user_card_status (user_id, card_id, problem_id, card_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, cardId)
                        .setParameter(3, problemId)
                        .setParameter(4, cardStatus)
                        .executeUpdate();
                log.info("user_card_status INSERT됨: userId={}, cardId={}, problemId={}, cardStatus={}", internalUserId, cardId, problemId, cardStatus);
            } else {
                entityManager.createNativeQuery(
                                "UPDATE user_card_status SET card_status = ?1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?2 AND card_id = ?3 AND problem_id = ?4")
                        .setParameter(1, cardStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, cardId)
                        .setParameter(4, problemId)
                        .executeUpdate();
                log.info("user_card_status UPDATE됨: userId={}, cardId={}, problemId={}, cardStatus={}", internalUserId, cardId, problemId, cardStatus);
            }

            String checkProblemStatusSql = "SELECT IFNULL(ucs.card_status, 'new') FROM cards c " +
                    "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND ucs.user_id = ?1 " +
                    "WHERE c.problem_id = ?2";
            List<String> currentProblemCardStatuses = entityManager.createNativeQuery(checkProblemStatusSql, String.class)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            String newProblemStatus = "completed";
            boolean hasAnyCardsForProblem = !currentProblemCardStatuses.isEmpty();

            if (!hasAnyCardsForProblem) {
                newProblemStatus = "new";
            } else {
                for (String status : currentProblemCardStatuses) {
                    if (!"perfect".equals(status)) {
                        newProblemStatus = "ongoing";
                        break;
                    }
                }
            }

            List<?> existingProblemStatus = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existingProblemStatus.isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, problem_status, is_liked, is_scrapped, created_at, updated_at) VALUES (?1, ?2, ?3, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newProblemStatus)
                        .executeUpdate();
                log.info("user_problem_status INSERT됨 (카드 상태 기반): userId={}, problemId={}, problemStatus={}", internalUserId, problemId, newProblemStatus);
            } else {
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET problem_status = ?1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, newProblemStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
                log.info("user_problem_status UPDATE됨 (카드 상태 기반): userId={}, problemId={}, problemStatus={}", internalUserId, problemId, newProblemStatus);
            }

            return ResponseEntity.ok(Map.of("status", "OK", "problemStatus", newProblemStatus));
        } catch (Exception e) {
            log.error("카드 학습 상태 및 문제 상태 업데이트 중 오류 발생 (SolveController): {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    // ⭐⭐ ADDED NEW ENDPOINT: /api/solve/set-ongoing ⭐⭐
    // This endpoint handles the transition from 'new' to 'ongoing' when "문제 바로 풀기" is clicked.
    @PostMapping("/solve/set-ongoing")
    public ResponseEntity<Map<String, Object>> setProblemStatusToOngoing(@RequestBody Map<String, Object> data) {
        String userId = (String) data.get("userId");
        Long problemId = ((Number) data.get("problemId")).longValue();

        log.info("setProblemStatusToOngoing 호출됨: userId={}, problemId={}", userId, problemId);

        if (userId == null || userId.trim().isEmpty() || problemId == null) {
            log.warn("setProblemStatusToOngoing: 유효하지 않은 입력값입니다. userId: {}, problemId: {}", userId, problemId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID 또는 문제 ID가 누락되었습니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("setProblemStatusToOngoing: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("setProblemStatusToOngoing: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            // Check if the record exists and update its status to 'ongoing'
            int updatedRows = entityManager.createNativeQuery(
                            "UPDATE user_problem_status SET problem_status = 'ongoing', updated_at = CURRENT_TIMESTAMP WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .executeUpdate();

            if (updatedRows > 0) {
                log.info("user_problem_status 업데이트됨: userId={}, problemId={}, status='ongoing'", internalUserId, problemId);
                return ResponseEntity.ok(Map.of("status", "OK", "problemStatus", "ongoing"));
            } else {
                log.warn("user_problem_status 업데이트 실패: 해당 userId={} problemId={} 조합의 레코드를 찾을 수 없습니다. (또는 이미 'ongoing' 상태)", internalUserId, problemId);
                // Depending on your business logic, you might want to insert a 'new' record here
                // if it's truly not found, but typically the 'start-study' endpoint handles initial creation.
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "문제 상태를 업데이트할 레코드를 찾을 수 없습니다."));
            }

        } catch (Exception e) {
            log.error("setProblemStatusToOngoing: 문제 상태 업데이트 중 오류 발생 (problemId: {}, userId: {}): {}", problemId, userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 사용자가 특정 문제 학습을 처음 시작할 때 호출됩니다.
     * user_problem_status 테이블에 해당 문제의 레코드가 없으면 새로 생성하고 'new' 상태로 초기화합니다.
     * 이미 존재하면 기존 상태를 반환합니다.
     * POST /api/solve/start-study
     *
     * @param data 사용자 ID (userId)와 문제 ID (problemId)를 포함하는 맵
     * @return 문제 학습 시작 결과 (problemStatus: "" 또는 기존 상태)
     */
    @PostMapping("/solve/start-study")
    @Transactional
    public ResponseEntity<Map<String, Object>> initiateProblemStudy(@RequestBody Map<String, Object> data) {
        String userId = (String) data.get("userId");
        Long problemId = ((Number) data.get("problemId")).longValue();

        log.info("initiateProblemStudy 호출됨: userId={}, problemId={}", userId, problemId);

        if (userId == null || userId.trim().isEmpty() || problemId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "ERROR", "message", "사용자 ID 또는 문제 ID가 누락되었습니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "ERROR", "message", "해당 userId에 해당하는 유저가 존재하지 않습니다."));
        } catch (Exception e) {
            log.error("서버 내부 오류 발생: 사용자 ID 조회 실패 - {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", "서버 오류로 사용자 조회에 실패했습니다."));
        }

        try {
            List<String> result = entityManager.createNativeQuery(
                            "SELECT problem_status FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2", String.class)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            String finalStatus;

            if (result.isEmpty()) {
                // 상태가 없으면 'new'로 저장하되, 응답은 ""로 (모달 표시 목적)
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, problem_status, is_liked, is_scrapped, created_at, updated_at) " +
                                        "VALUES (?1, ?2, 'new', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .executeUpdate();

                log.info("신규 user_problem_status 레코드 삽입 완료");
                finalStatus = ""; // ✅ 프론트가 모달 뜨게
            } else {
                // 이미 있으면 상태만 'new'로 덮어쓰기
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET problem_status = 'new', updated_at = CURRENT_TIMESTAMP WHERE user_id = ?1 AND problem_id = ?2")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .executeUpdate();

                log.info("기존 레코드 상태를 'new'로 업데이트 완료");
                finalStatus = "new"; // ✅ 프론트는 바로 페이지 이동
            }

            return ResponseEntity.ok(Map.of("status", "OK", "problemStatus", finalStatus));

        } catch (Exception e) {
            log.error("initiateProblemStudy: 학습 상태 처리 중 오류 발생 - {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

}