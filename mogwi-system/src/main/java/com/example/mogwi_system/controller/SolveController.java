// src/main/java/com/example/mogwi_system/controller/SolveController.java

package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api") // All endpoints still rooted under /api as per your request
public class SolveController { // Class name remains SolveController

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 특정 문제(problemId)에 속한 카드 목록과 해당 사용자의 학습 상태를 조회합니다.
     * GET /api/study/{problemId}/solve
     * (Endpoint path as originally provided for this method)
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
                    "SELECT c.id, c.question, c.correct, c.image_url, " + // ✅ image_url is still here
                            "IFNULL(ucs.card_status, 'new') AS card_status " +
                            "FROM cards c " +
                            // Crucial: Use c.problem_id for the JOIN condition on ucs.problem_id
                            "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND ucs.problem_id = c.problem_id ");

            // Only add user_id condition if internalUserId is valid
            if (internalUserId != null) {
                // Using positional parameter for internalUserId here as well
                sql.append("AND ucs.user_id = ?2 "); // Renamed parameter placeholder for clarity in this query
            }

            sql.append("WHERE c.problem_id = ?1 " + // Renamed parameter placeholder for clarity in this query
                    "ORDER BY c.id ASC");


            var queryObj = entityManager.createNativeQuery(sql.toString());
            queryObj.setParameter(1, problemId); // Set problemId to ?1
            if (internalUserId != null) {
                queryObj.setParameter(2, internalUserId); // Set internalUserId to ?2
            }

            List<Object[]> results = queryObj.getResultList();
            List<Map<String, Object>> cards = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("id", ((Number) row[0]).longValue());
                card.put("question", row[1]);
                card.put("correct", row[2]);
                card.put("imageUrl", row[3]); // ✅ 프론트엔드로 이미지 URL 전달
                card.put("cardStatus", row[4].toString());
                cards.add(card);
            }

            if (cards.isEmpty()) {
                // Changed to 204 No Content, which is more appropriate for an empty list
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }

            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("문제 카드 및 학습 상태 조회 중 오류 발생 (SolveController): {}", e.getMessage()); // Updated log message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 특정 카드의 사용자 학습 상태를 업데이트합니다.
     * POST /api/solve/{cardId}/status
     * (Endpoint path as originally provided for this method)
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
        String cardStatus = (String) data.get("cardStatus"); // "perfect", "vague", "forgotten"
        Long problemId = ((Number) data.get("problemId")).longValue();

        // Input validation for the allowed card statuses
        if (userId == null || cardStatus == null || cardId == null || problemId == null ||
                (!cardStatus.equals("perfect") && !cardStatus.equals("vague") && !cardStatus.equals("forgotten"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "유효하지 않은 카드 상태 또는 입력값입니다."));
        }

        try {
            // 1. 사용자 내부 ID 조회
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getResultList();

            if (userResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
            }
            Long internalUserId = ((Number) userResult.get(0)).longValue();

            // 2. user_card_status 테이블에 상태 저장 또는 업데이트
            List<?> existingCardStatus = entityManager.createNativeQuery(
                            "SELECT id FROM user_card_status WHERE user_id = ?1 AND card_id = ?2 AND problem_id = ?3")
                    .setParameter(1, internalUserId)
                    .setParameter(2, cardId)
                    .setParameter(3, problemId)
                    .getResultList();

            if (existingCardStatus.isEmpty()) {
                // 새로운 상태이므로 INSERT.
                entityManager.createNativeQuery(
                                "INSERT INTO user_card_status (user_id, card_id, problem_id, card_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, cardId)
                        .setParameter(3, problemId)
                        .setParameter(4, cardStatus)
                        .executeUpdate();
            } else {
                // 이미 존재하는 상태이므로 UPDATE. Using positional parameters as requested.
                entityManager.createNativeQuery(
                                "UPDATE user_card_status SET card_status = ?1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?2 AND card_id = ?3 AND problem_id = ?4")
                        .setParameter(1, cardStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, cardId)
                        .setParameter(4, problemId)
                        .executeUpdate();
            }

            // 3. user_problem_status 테이블의 상태 갱신 로직 (Based on all cards for this problem)
            // Retrieve all card statuses for the given problem and user. Using positional parameters.
            String checkProblemStatusSql = "SELECT IFNULL(ucs.card_status, 'new') FROM cards c " +
                    "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND ucs.user_id = ?1 " +
                    "WHERE c.problem_id = ?2"; // Adjusted parameter index
            List<String> currentProblemCardStatuses = entityManager.createNativeQuery(checkProblemStatusSql, String.class)
                    .setParameter(1, internalUserId) // Set internalUserId to ?1
                    .setParameter(2, problemId)      // Set problemId to ?2
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

            // Insert or update the user_problem_status table. Using positional parameters.
            List<?> existingProblemStatus = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existingProblemStatus.isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, status, is_liked, is_scrapped, created_at, updated_at) VALUES (?1, ?2, ?3, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newProblemStatus)
                        .executeUpdate();
            } else {
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET status = ?1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, newProblemStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK", "problemStatus", newProblemStatus));
        } catch (Exception e) {
            log.error("카드 학습 상태 및 문제 상태 업데이트 중 오류 발생 (SolveController): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }
}