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

    /**
     * 특정 사용자의 전체 학습 요약 통계를 조회합니다.
     * GET /api/mystudy/summary/{userId}
     * (예: 완벽, 모호, 망각 카운트)
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 전체 학습 요약 데이터
     */
    @GetMapping("/summary/{userId}")
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
     * 특정 사용자의 각 문제별 학습 상세 현황 (카드 상태별 개수, 작성자 닉네임, 태그 포함)을 조회합니다.
     * GET /api/mystudy/problems/detail/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 사용자의 문제 목록, 각 문제별 카드 학습 현황, 작성자 닉네임, 태그 포함
     */
    @GetMapping("/problems/detail/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserStudyProblemsDetail(
            @PathVariable String userId) {
        log.info("MyStudyController - getUserStudyProblemsDetail 호출됨: userId={}", userId);
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("MyStudyController - getUserStudyProblemsDetail: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - getUserStudyProblemsDetail: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("MyStudyController - getUserStudyProblemsDetail: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            String problemSql = "SELECT " +
                    "p.id AS problem_id, " +
                    "p.title, " +
                    "p.description, " +
                    "p.card_count AS problem_total_cards, " +
                    "u.username AS author_nickname, " +
                    "COALESCE(ups.is_liked, 0) AS is_liked, " +
                    "COALESCE(ups.is_scrapped, 0) AS is_scrapped, " +
                    "IFNULL(ups.problem_status, 'new') AS study_status, " +
                    "(SELECT COUNT(*) FROM user_problem_status ups_likes WHERE ups_likes.problem_id = p.id AND ups_likes.is_liked = 1) AS total_likes, " +
                    "(SELECT COUNT(*) FROM user_problem_status ups_scraps WHERE ups_scraps.problem_id = p.id AND ups_scraps.is_scrapped = 1) AS total_scraps, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS perfect_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS vague_count, " +
                    "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' AND ucs.problem_id = p.id THEN 1 ELSE 0 END), 0) AS forgotten_count " +
                    "FROM problems p " +
                    "JOIN users u ON p.author_id = u.id " +
                    "LEFT JOIN user_problem_status ups ON p.id = ups.problem_id AND ups.user_id = ?1 " +
                    "LEFT JOIN user_card_status ucs ON p.id = ucs.problem_id AND ucs.user_id = ?2 " + // 여기도 ?1 -> ?2 로 변경
                    "WHERE p.id IN (" +
                    "SELECT p_all.id FROM problems p_all " +
                    "LEFT JOIN user_problem_status ups_all ON p_all.id = ups_all.problem_id AND ups_all.user_id = ?3 " + // 여기도 ?1 -> ?3 로 변경
                    "WHERE ups_all.user_id IS NULL OR ups_all.problem_status IN ('new', 'ongoing', 'completed')" +
                    ") " +
                    "GROUP BY p.id, p.title, p.description, p.card_count, u.username, ups.is_liked, ups.is_scrapped, ups.problem_status " +
                    "ORDER BY IFNULL(ups.updated_at, p.created_at) DESC";

            List<Object[]> problemResults = entityManager.createNativeQuery(problemSql)
                    .setParameter(1, internalUserId) // 첫 번째 ?1 (ups.user_id)
                    .setParameter(2, internalUserId) // 두 번째 ?2 (ucs.user_id)
                    .setParameter(3, internalUserId) // 세 번째 ?3 (ups_all.user_id)
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
                problem.put("isLiked", ((Number) row[5]).intValue() == 1);
                problem.put("isScrapped", ((Number) row[6]).intValue() == 1);
                String studyStatus = row[7].toString();
                problem.put("studyStatus", studyStatus);
                problem.put("isCompleted", "completed".equals(studyStatus));
                problem.put("totalLikes", ((Number) row[8]).intValue());
                problem.put("totalScraps", ((Number) row[9]).intValue());
                problem.put("perfectCount", ((Number) row[10]).intValue());
                problem.put("vagueCount", ((Number) row[11]).intValue());
                problem.put("forgottenCount", ((Number) row[12]).intValue());

                String tagsSql = "SELECT c.tag_name FROM categories c " +
                        "JOIN problem_categories pc ON c.id = pc.category_id " +
                        "WHERE pc.problem_id = ?1";
                List<String> tags = entityManager.createNativeQuery(tagsSql)
                        .setParameter(1, problemId)
                        .getResultList();
                problem.put("categories", tags);

                userProblems.add(problem);
            }
            log.info("MyStudyController - getUserStudyProblemsDetail 성공: userId={}에 대해 {}개의 문제 조회됨.", userId, userProblems.size());
            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("MyStudyController - getUserStudyProblemsDetail: 사용자 문제 학습 상세 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * 문제의 '좋아요' 또는 '스크랩' 상태를 토글합니다.
     * POST /api/mystudy/problems/{problemId}/toggle-status
     *
     * @param problemId 토글할 문제의 ID
     * @param data      사용자 ID (userId), 토글할 필드 ("isLiked" 또는 "isScrapped")를 포함하는 맵
     * @return 처리 결과 상태 (OK 또는 ERROR)
     */
    @PostMapping("/problems/{problemId}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleProblemStatus(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {

        String userId = (String) data.get("userId");
        String fieldToToggle = (String) data.get("field");

        log.info("MyStudyController - toggleProblemStatus 호출됨: userId={}, problemId={}, field: {}", userId, problemId, fieldToToggle);

        if (userId == null || userId.trim().isEmpty() || fieldToToggle == null || problemId == null ||
                (!fieldToToggle.equals("isLiked") && !fieldToToggle.equals("isScrapped"))) {
            log.warn("MyStudyController - toggleProblemStatus: 유효하지 않은 입력 파라미터 - userId: {}, field: {}, problemId: {}", userId, fieldToToggle, problemId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "유효하지 않은 입력값입니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - toggleProblemStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            List<Object[]> existingStatusResults = entityManager.createNativeQuery(
                            "SELECT id, is_liked, is_scrapped, problem_status FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            boolean newValue = false;

            if (existingStatusResults.isEmpty()) {
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, is_liked, is_scrapped, problem_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, 'new', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

                boolean initialIsLiked = fieldToToggle.equals("isLiked");
                boolean initialIsScrapped = fieldToToggle.equals("isScrapped");
                newValue = initialIsLiked || initialIsScrapped;

                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, initialIsLiked ? 1 : 0)
                        .setParameter(4, initialIsScrapped ? 1 : 0)
                        .executeUpdate();
                log.info("MyStudyController - toggleProblemStatus: 새로운 user_problem_status 레코드 삽입됨. userId={}, problemId={}, {}: {}", internalUserId, problemId, fieldToToggle, newValue);

            } else {
                Object[] row = existingStatusResults.get(0);
                Long statusId = ((Number) row[0]).longValue();
                int currentIsLiked = (int) row[1];
                int currentIsScrapped = (int) row[2];

                String updateSql;

                if (fieldToToggle.equals("isLiked")) {
                    newValue = (currentIsLiked == 0);
                    updateSql = "UPDATE user_problem_status SET is_liked = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                    log.info("MyStudyController - toggleProblemStatus: is_liked 업데이트됨. userId={}, problemId={}. 새 상태: {}", internalUserId, problemId, newValue);
                } else {
                    newValue = (currentIsScrapped == 0);
                    updateSql = "UPDATE user_problem_status SET is_scrapped = ?1, updated_at = CURRENT_TIMESTAMP WHERE id = ?2";
                    entityManager.createNativeQuery(updateSql)
                            .setParameter(1, newValue ? 1 : 0)
                            .setParameter(2, statusId)
                            .executeUpdate();
                    log.info("MyStudyController - toggleProblemStatus: is_scrapped 업데이트됨. userId={}, problemId={}. 새 상태: {}", internalUserId, problemId, newValue);
                }
            }

            String countsSql = "SELECT " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups_likes WHERE ups_likes.problem_id = ?1 AND ups_likes.is_liked = 1), 0) AS total_likes, " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups_scraps WHERE ups_scraps.problem_id = ?1 AND ups_scraps.is_scrapped = 1), 0) AS total_scraps";

            Object[] countsResult = (Object[]) entityManager.createNativeQuery(countsSql)
                    .setParameter(1, problemId)
                    .getSingleResult();

            int totalLikes = ((Number) countsResult[0]).intValue();
            int totalScraps = ((Number) countsResult[1]).intValue();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("newStatus", newValue);
            response.put("totalLikes", totalLikes);
            response.put("totalScraps", totalScraps);

            log.info("MyStudyController - toggleProblemStatus 성공: userId={}, problemId={}. 새 총계: 좋아요={}, 스크랩={}", userId, problemId, totalLikes, totalScraps);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemStatus: 문제 '좋아요' 또는 '스크랩' 상태 토글 중 오류 발생 (problemId: {}, userId: {}): {}", problemId, userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 나의 학습에서 특정 문제에 대한 학습 현황을 삭제합니다.
     * 이 엔드포인트는 user_card_status와 user_problem_status 테이블에서 관련 레코드를 삭제합니다.
     * DELETE /api/mystudy/problems/{problemId}/user/{userId}
     *
     * @param problemId 삭제할 문제의 ID
     * @param userId    학습 현황을 삭제할 사용자의 외부 ID (users 테이블의 userid 필드)
     * @return 삭제 결과 상태
     */
    @DeleteMapping("/problems/{problemId}/user/{userId}") // 엔드포인트 경로를 /problems/{problemId}/user/{userId}로 변경
    public ResponseEntity<Map<String, Object>> deleteUserProblemStatus(
            @PathVariable Long problemId,
            @PathVariable String userId) {

        log.info("MyStudyController - deleteUserProblemStatus 호출됨: userId={}, problemId={}", userId, problemId);

        if (userId == null || userId.trim().isEmpty() || problemId == null) {
            log.warn("MyStudyController - deleteUserProblemStatus: 유효하지 않은 입력값입니다. userId: {}, problemId: {}", userId, problemId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", "사용자 ID 또는 문제 ID가 누락되었습니다."));
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - deleteUserProblemStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("MyStudyController - deleteUserProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }

        try {
            // 먼저, 이 사용자와 문제에 대한 모든 카드 상태를 삭제합니다.
            int deletedCardStatuses = entityManager.createNativeQuery(
                            "DELETE FROM user_card_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .executeUpdate();
            log.info("MyStudyController - user_card_status에서 {}개의 레코드 삭제됨 (userId={}, problemId={})", deletedCardStatuses, internalUserId, problemId);

            // 다음으로, 주요 user_problem_status 레코드를 삭제합니다.
            int deletedProblemStatus = entityManager.createNativeQuery(
                            "DELETE FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .executeUpdate();

            if (deletedProblemStatus > 0) {
                log.info("MyStudyController - user_problem_status 삭제 성공: userId={}, problemId={}", internalUserId, problemId);
                return ResponseEntity.ok(Map.of("status", "OK", "message", "문제 학습 현황이 성공적으로 삭제되었습니다."));
            } else {
                log.warn("MyStudyController - user_problem_status 삭제 실패: 해당 userId={} problemId={} 조합의 레코드를 찾을 수 없습니다.", internalUserId, problemId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "삭제할 문제 학습 현황을 찾을 수 없습니다."));
            }

        } catch (Exception e) {
            log.error("MyStudyController - 문제 학습 현황 삭제 중 오류 발생 (problemId: {}, userId: {}): {}", problemId, userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류: " + e.getMessage()));
        }
    }
}