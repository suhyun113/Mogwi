package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class LikeScrapController {

    @PersistenceContext
    private EntityManager entityManager;

    /** 좋아요 토글 */
    @PostMapping("/like/{problemId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long problemId, @RequestBody Map<String, String> data) {
        String userId = data.get("userId");
        try {
            Long internalUserId = getInternalUserId(userId);
            Integer isLiked = getOrCreateStatus("is_liked", internalUserId, problemId);
            int newStatus = (isLiked == 1) ? 0 : 1;
            updateStatus("is_liked", newStatus, internalUserId, problemId);
            return ResponseEntity.ok(Map.of("status", "OK", "isLiked", newStatus == 1));
        } catch (Exception e) {
            log.error("좋아요 토글 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** 스크랩 토글 */
    @PostMapping("/scrap/{problemId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleScrap(@PathVariable Long problemId, @RequestBody Map<String, String> data) {
        String userId = data.get("userId");
        try {
            Long internalUserId = getInternalUserId(userId);
            Integer isScrapped = getOrCreateStatus("is_scrapped", internalUserId, problemId);
            int newStatus = (isScrapped == 1) ? 0 : 1;
            updateStatus("is_scrapped", newStatus, internalUserId, problemId);
            return ResponseEntity.ok(Map.of("status", "OK", "isScrapped", newStatus == 1));
        } catch (Exception e) {
            log.error("스크랩 토글 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** 좋아요 여부 확인 */
    @GetMapping("/like-status/{problemId}/{userId}")
    public ResponseEntity<Map<String, Object>> getLikeStatus(@PathVariable Long problemId, @PathVariable String userId) {
        try {
            Long internalUserId = getInternalUserId(userId);
            Integer isLiked = getStatus("is_liked", internalUserId, problemId);
            return ResponseEntity.ok(Map.of("status", "OK", "isLiked", isLiked == 1));
        } catch (NoResultException e) {
            return ResponseEntity.ok(Map.of("status", "OK", "isLiked", false));
        } catch (Exception e) {
            log.error("좋아요 상태 조회 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** 스크랩 여부 확인 */
    @GetMapping("/scrap-status/{problemId}/{userId}")
    public ResponseEntity<Map<String, Object>> getScrapStatus(@PathVariable Long problemId, @PathVariable String userId) {
        try {
            Long internalUserId = getInternalUserId(userId);
            Integer isScrapped = getStatus("is_scrapped", internalUserId, problemId);
            return ResponseEntity.ok(Map.of("status", "OK", "isScrapped", isScrapped == 1));
        } catch (NoResultException e) {
            return ResponseEntity.ok(Map.of("status", "OK", "isScrapped", false));
        } catch (Exception e) {
            log.error("스크랩 상태 조회 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** 좋아요/스크랩한 문제 목록 조회 */
    @GetMapping("/problems/liked-scraped/{userId}")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getLikedScrapedProblems(@PathVariable String userId) {
        Map<String, List<Map<String, Object>>> response = new HashMap<>();
        List<Map<String, Object>> likedProblems = new ArrayList<>();
        List<Map<String, Object>> scrapedProblems = new ArrayList<>();

        try {
            Long internalUserId = getInternalUserId(userId);
            String sql = "SELECT p.id, p.title, p.is_public, ups.is_liked, ups.is_scrapped " +
                    "FROM problems p JOIN user_problem_status ups ON p.id = ups.problem_id " +
                    "WHERE ups.user_id = ?1 AND (ups.is_liked = 1 OR ups.is_scrapped = 1) " +
                    "ORDER BY ups.updated_at DESC";

            List<Object[]> results = entityManager.createNativeQuery(sql).setParameter(1, internalUserId).getResultList();

            for (Object[] row : results) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                problem.put("is_public", ((Number) row[2]).intValue() == 1);

                String topicSql = "SELECT c.tag_name FROM categories c JOIN problem_categories pc ON c.id = pc.category_id WHERE pc.problem_id = ?1 LIMIT 1";
                try {
                    String topic = (String) entityManager.createNativeQuery(topicSql).setParameter(1, problemId).getSingleResult();
                    problem.put("topic", topic);
                } catch (NoResultException e) {
                    problem.put("topic", null);
                }

                if (Boolean.TRUE.equals((Boolean) row[3])) likedProblems.add(problem);
                if (Boolean.TRUE.equals((Boolean) row[4])) scrapedProblems.add(problem);
            }

            response.put("likedProblems", likedProblems);
            response.put("scrapedProblems", scrapedProblems);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("좋아요/스크랩 문제 목록 조회 오류: {}", e.getMessage(), e);
            response.put("likedProblems", likedProblems);
            response.put("scrapedProblems", scrapedProblems);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** 내부 유저 ID 조회 */
    private Long getInternalUserId(String userId) {
        return ((Number) entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                .setParameter(1, userId)
                .getSingleResult()).longValue();
    }

    /** 상태 조회 (단순 조회) */
    private Integer getStatus(String column, Long userId, Long problemId) {
        Object result = entityManager.createNativeQuery(
                        "SELECT " + column + " FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                .setParameter(1, userId)
                .setParameter(2, problemId)
                .getSingleResult();

        if (result instanceof Number) return ((Number) result).intValue();
        if (result instanceof Boolean) return ((Boolean) result ? 1 : 0);
        throw new IllegalStateException("Unexpected type: " + result.getClass());
    }

    /** 상태 조회 또는 새 레코드 생성 후 기본값 반환 */
    private Integer getOrCreateStatus(String column, Long userId, Long problemId) {
        try {
            return getStatus(column, userId, problemId);
        } catch (NoResultException e) {
            // 새 레코드 삽입
            entityManager.createNativeQuery(
                            "INSERT INTO user_problem_status (user_id, problem_id, is_liked, is_scrapped, problem_status, created_at, updated_at) " +
                                    "VALUES (?1, ?2, 0, 0, 'new', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .setParameter(1, userId)
                    .setParameter(2, problemId)
                    .executeUpdate();
            return 0;
        }
    }

    /** 상태 업데이트 */
    private void updateStatus(String column, int status, Long userId, Long problemId) {
        entityManager.createNativeQuery("UPDATE user_problem_status SET " + column + " = ?1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?2 AND problem_id = ?3")
                .setParameter(1, status)
                .setParameter(2, userId)
                .setParameter(3, problemId)
                .executeUpdate();
    }
}
