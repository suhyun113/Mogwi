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
import java.util.stream.Collectors; // Unused, can be removed if not used elsewhere

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/mystudy") // Base path for MyStudyController endpoints
public class MyStudyController {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper method to get internal user ID from userid
    private Long getInternalUserId(String userId) throws NoResultException {
        // Log the userId being looked up
        log.info("Attempting to get internal ID for external userId: {}", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("No internal user found for external userId: {}", userId);
            throw e; // Re-throw to be caught by the calling method's specific handler
        } catch (Exception e) {
            log.error("Error getting internal user ID for external userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve internal user ID", e); // Wrap other exceptions
        }
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
                log.warn("getOverallStudySummary: Received null or empty userId.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID가 누락되었습니다."));
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("getOverallStudySummary: 사용자 ID '{}'를 찾을 수 없음.", userId);
            // If user ID is not found, return 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("getOverallStudySummary: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
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
     * 특정 사용자의 각 문제별 학습 상세 현황 (카드 상태별 개수, 작성자 닉네임, 태그 포함)을 조회합니다.
     * GET /api/mystudy/problems/detail/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 사용자의 문제 목록, 각 문제별 카드 학습 현황, 작성자 닉네임, 태그 포함
     */
    @GetMapping("/problems/detail/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserStudyProblemsDetail(
            @PathVariable String userId) {
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("getUserStudyProblemsDetail: Received null or empty userId.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("getUserStudyProblemsDetail: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("getUserStudyProblemsDetail: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            // 메인 문제 정보와 유저 상태 정보 조회 쿼리
            // user_problem_status의 problem_status는 기본값이 'new'이므로, 'ongoing' 또는 'completed'인 경우만 가져오도록 수정
            String problemSql = "SELECT " +
                    "p.id AS problem_id, " +
                    "p.title, " +
                    "p.description, " +
                    "p.card_count AS problem_total_cards, " +
                    "u.username AS author_nickname, " + // users 테이블의 username 사용 (닉네임으로 가정)
                    "COALESCE(ups.is_liked, 0) AS is_liked, " + // user_problem_status의 is_liked 사용, NULL이면 0
                    "COALESCE(ups.is_scrapped, 0) AS is_scrapped, " + // user_problem_status의 is_scrapped 사용, NULL이면 0
                    "IFNULL(ups.problem_status, 'new') AS study_status, " + // problem_status, NULL이면 'new'
                    // 총 좋아요 수와 총 스크랩 수 추가
                    "(SELECT COUNT(*) FROM user_problem_status ups_likes WHERE ups_likes.problem_id = p.id AND ups_likes.is_liked = 1) AS total_likes, " +
                    "(SELECT COUNT(*) FROM user_problem_status ups_scraps WHERE ups_scraps.problem_id = p.id AND ups_scraps.is_scrapped = 1) AS total_scraps, " +
                    // 각 문제별 카드 학습 상태 카운트 추가 (perfectCount, vagueCount, forgottenCount)
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS perfect_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS vague_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS forgotten_count " +
                    "FROM problems p " +
                    "JOIN users u ON p.author_id = u.id " + // users 테이블 조인
                    "LEFT JOIN user_problem_status ups ON p.id = ups.problem_id AND ups.user_id = ?1 " + // user_problem_status LEFT JOIN
                    "LEFT JOIN user_card_status ucs ON p.id = ucs.problem_id AND ucs.user_id = ?1 " + // user_card_status LEFT JOIN
                    "WHERE ups.user_id = ?1 " + // 현재 사용자가 학습한 문제만 포함하도록 명시적으로 필터링
                    "GROUP BY p.id, p.title, p.description, p.card_count, u.username, ups.is_liked, ups.is_scrapped, ups.problem_status " + // 그룹 바이 추가
                    "ORDER BY p.id ASC";

            List<Object[]> problemResults = entityManager.createNativeQuery(problemSql)
                    .setParameter(1, internalUserId)
                    .getResultList();

            List<Map<String, Object>> userProblems = new ArrayList<>();

            for (Object[] row : problemResults) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                problem.put("description", row[2]);
                problem.put("cardCount", ((Number) row[3]).intValue());
                problem.put("authorNickname", row[4]);
                problem.put("isLiked", ((Number) row[5]).intValue() == 1); // is_liked (boolean)
                problem.put("isScrapped", ((Number) row[6]).intValue() == 1); // is_scrapped (boolean)
                problem.put("studyStatus", row[7].toString());
                problem.put("isCompleted", "completed".equals(row[7].toString()));
                problem.put("totalLikes", ((Number) row[8]).intValue());   // 총 좋아요 수
                problem.put("totalScraps", ((Number) row[9]).intValue()); // 총 스크랩 수
                // 추가된 카드 학습 상태 카운트
                problem.put("perfectCount", ((Number) row[10]).intValue());
                problem.put("vagueCount", ((Number) row[11]).intValue());
                problem.put("forgottenCount", ((Number) row[12]).intValue());


                // 해당 문제의 태그(카테고리) 조회
                String tagsSql = "SELECT c.tag_name FROM categories c " +
                        "JOIN problem_categories pc ON c.id = pc.category_id " +
                        "WHERE pc.problem_id = ?1";
                List<String> tags = entityManager.createNativeQuery(tagsSql)
                        .setParameter(1, problemId)
                        .getResultList();
                problem.put("categories", tags);

                userProblems.add(problem);
            }

            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("getUserStudyProblemsDetail: 사용자 문제 학습 상세 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

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
            log.warn("toggleProblemStatus: Invalid input parameters - userId: {}, field: {}, problemId: {}", userId, fieldToToggle, problemId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "유효하지 않은 입력값입니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("toggleProblemStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("toggleProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            // user_problem_status 레코드 조회 시 problem_status 필드도 함께 가져옴
            List<?> existingStatus = entityManager.createNativeQuery(
                            "SELECT id, is_liked, is_scrapped, problem_status FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            boolean newValue = false; // Will store the new toggled boolean status

            if (existingStatus.isEmpty()) {
                // INSERT 시 problem_status의 기본값은 'new'로 설정. created_at, updated_at도 추가.
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, is_liked, is_scrapped, problem_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, 'new', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                boolean newIsLiked = fieldToToggle.equals("isLiked");
                boolean newIsScrapped = fieldToToggle.equals("isScrapped");

                newValue = (newIsLiked || newIsScrapped); // The new status for the toggled field

                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newIsLiked ? 1 : 0)
                        .setParameter(4, newIsScrapped ? 1 : 0)
                        .executeUpdate();

            } else {
                Object[] row = (Object[]) existingStatus.get(0);
                Long statusId = ((Number) row[0]).longValue();
                int currentIsLiked = (int) row[1];
                int currentIsScrapped = (int) row[2];

                String updateSql;

                if (fieldToToggle.equals("isLiked")) {
                    newValue = (currentIsLiked == 0); // Toggle logic
                    updateSql = "UPDATE user_problem_status SET is_liked = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                } else { // "isScrapped"
                    newValue = (currentIsScrapped == 0); // Toggle logic
                    updateSql = "UPDATE user_problem_status SET is_scrapped = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                }
            }

            // After insert/update, retrieve the current total like and scrap counts for the problem
            String countsSql = "SELECT " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups_likes WHERE ups_likes.problem_id = ?1 AND ups_likes.is_liked = 1), 0) AS total_likes, " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups_scraps WHERE ups_scraps.problem_id = ?1 AND ups_scraps.is_scrapped = 1), 0) AS total_scraps";

            Object[] countsResult = (Object[]) entityManager.createNativeQuery(countsSql)
                    .setParameter(1, problemId)
                    .getSingleResult();

            int totalLikes = ((Number) countsResult[0]).intValue();
            int totalScraps = ((Number) countsResult[1]).intValue();

            // Return the updated status and the new counts
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("newStatus", newValue); // The new boolean state of isLiked/isScrapped for the current user
            response.put("totalLikes", totalLikes);
            response.put("totalScraps", totalScraps);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("toggleProblemStatus: 문제 '좋아요' 또는 '스크랩' 상태 토글 중 오류 발생 (problemId: {}, userId: {}): {}", problemId, userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }
}