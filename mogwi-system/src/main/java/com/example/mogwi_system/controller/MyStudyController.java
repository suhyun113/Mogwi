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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/mystudy") // Base path for MyStudyController endpoints
public class MyStudyController {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper method to get internal user ID from userid
    private Long getInternalUserId(String userId) throws NoResultException {
        return ((Number) entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                .setParameter(1, userId)
                .getSingleResult()).longValue();
    }

    /**
     * 특정 사용자의 전체 학습 요약 통계를 조회합니다.
     * GET /api/mystudy/summary/{userId}
     * (e.g., perfect, vague, forgotten counts)
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 전체 학습 요약 데이터
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<Map<String, Object>> getOverallStudySummary(
            @PathVariable String userId) {
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID가 누락되었습니다."));
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("getOverallStudySummary: 사용자 ID를 찾을 수 없음: {}", userId);
            // If user ID is not found, return 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("getOverallStudySummary: 사용자 ID 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
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

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("getOverallStudySummary: 학습 요약 데이터 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 특정 사용자의 각 문제별 학습 상세 현황 (카드 상태별 개수)을 조회합니다.
     * GET /api/mystudy/problems/detail/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 사용자의 문제 목록, 각 문제별 카드 학습 현황 포함
     */
    @GetMapping("/problems/detail/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserStudyProblemsDetail(
            @PathVariable String userId) {
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("getUserStudyProblemsDetail: 사용자 ID를 찾을 수 없음: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("getUserStudyProblemsDetail: 사용자 ID 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            String sql = "SELECT " +
                    "p.id AS problem_id, " +
                    "p.title, " +
                    "p.description, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' THEN 1 ELSE 0 END), 0) AS perfect_cards, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' THEN 1 ELSE 0 END), 0) AS vague_cards, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' THEN 1 ELSE 0 END), 0) AS forgotten_cards, " +
                    "COALESCE(COUNT(ucs.id), 0) AS total_learned_cards, " +
                    "p.card_count AS problem_total_cards, " +
                    "IFNULL(ups.problem_status, 'new') AS study_status " + // p.image_url 제거
                    "FROM problems p " +
                    "JOIN user_problem_status ups ON p.id = ups.problem_id " +
                    "LEFT JOIN cards c ON p.id = c.problem_id " +
                    "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND ucs.problem_id = c.problem_id AND ucs.user_id = ups.user_id " +
                    "WHERE ups.user_id = ?1 " +
                    "GROUP BY p.id, p.title, p.description, p.card_count, ups.problem_status " + // p.image_url 제거
                    "ORDER BY p.id ASC";

            var queryObj = entityManager.createNativeQuery(sql);
            queryObj.setParameter(1, internalUserId);

            List<Object[]> results = queryObj.getResultList();
            List<Map<String, Object>> userProblems = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> problem = new HashMap<>();
                problem.put("id", ((Number) row[0]).longValue());
                problem.put("title", row[1]);
                problem.put("description", row[2]);
                problem.put("perfectCards", ((Number) row[3]).intValue());
                problem.put("vagueCards", ((Number) row[4]).intValue());
                problem.put("forgottenCards", ((Number) row[5]).intValue());
                problem.put("totalLearnedCards", ((Number) row[6]).intValue());
                problem.put("totalCards", ((Number) row[7]).intValue());
                problem.put("studyStatus", row[8].toString());
                problem.put("isCompleted", "completed".equals(row[8].toString()));
                // problem.put("imageUrl", row[9] != null ? row[9].toString() : null); // p.image_url 제거에 따라 이 라인도 제거

                userProblems.add(problem);
            }

            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("getUserStudyProblemsDetail: 사용자 문제 학습 상세 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    // Keep existing toggle-status API for liked/scrapped from previous MyStudyController
    /**
     * MyStudyController의 toggleStatus는 이제 MyStudyView에서 좋아요/스크랩을 토글할 때 사용됩니다.
     * ProblemController의 toggleLike/Scrap과는 다릅니다. ProblemController의 것은 메인 문제 목록에서 사용됩니다.
     * POST /api/my-study/problems/{problemId}/toggle-status
     *
     * @param problemId 토글할 문제의 ID
     * @param data 사용자 ID (userId), 토글할 필드 ("isLiked" 또는 "isScrapped")를 포함하는 맵
     * @return 처리 결과 상태 (OK 또는 ERROR)
     */
    @PostMapping("/problems/{problemId}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleProblemStatus(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {

        String userId = (String) data.get("userId");
        String fieldToToggle = (String) data.get("field"); // "isLiked" or "isScrapped"

        if (userId == null || userId.trim().isEmpty() || fieldToToggle == null || problemId == null ||
                (!fieldToToggle.equals("isLiked") && !fieldToToggle.equals("isScrapped"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "유효하지 않은 입력값입니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("toggleProblemStatus: 사용자 ID를 찾을 수 없음: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("toggleProblemStatus: 사용자 ID 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            List<?> existingStatus = entityManager.createNativeQuery(
                            "SELECT id, is_liked, is_scrapped, problem_status FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existingStatus.isEmpty()) {
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, problem_status, is_liked, is_scrapped, created_at, updated_at) VALUES (?1, ?2, 'new', ?3, ?4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                boolean newIsLiked = fieldToToggle.equals("isLiked");
                boolean newIsScrapped = fieldToToggle.equals("isScrapped");
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newIsLiked ? 1 : 0)
                        .setParameter(4, newIsScrapped ? 1 : 0)
                        .executeUpdate();

                return ResponseEntity.ok(Map.of("status", "OK", "newStatus", newIsLiked || newIsScrapped));

            } else {
                Object[] row = (Object[]) existingStatus.get(0);
                Long statusId = ((Number) row[0]).longValue();
                int currentIsLiked = (int) row[1];
                int currentIsScrapped = (int) row[2];

                String updateSql;
                boolean newValue;

                if (fieldToToggle.equals("isLiked")) {
                    newValue = (currentIsLiked == 0);
                    updateSql = "UPDATE user_problem_status SET is_liked = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                } else { // "isScrapped"
                    newValue = (currentIsScrapped == 0);
                    updateSql = "UPDATE user_problem_status SET is_scrapped = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                }
                return ResponseEntity.ok(Map.of("status", "OK", "newStatus", newValue));
            }

        } catch (Exception e) {
            log.error("toggleProblemStatus: 문제 '좋아요' 또는 '스크랩' 상태 토글 중 오류 발생 (problemId: {}): {}", problemId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }
}