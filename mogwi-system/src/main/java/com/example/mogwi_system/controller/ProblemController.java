package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Transactional
public class ProblemController {

    @PersistenceContext
    private EntityManager entityManager;

    // 문제 목록 조회 API
    @GetMapping("/api/problems")
    public ResponseEntity<List<Map<String, Object>>> getProblems(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String currentUserId
    ) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, u.username AS author_name, u.userid AS author_id, p.card_count, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1), 0) AS likes, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_scrapped = 1), 0) AS scraps, " +
                            "IFNULL(ups.is_liked, 0) AS liked, " +
                            "IFNULL(ups.is_scrapped, 0) AS scrapped, " +
                            "c.tag_name AS category " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id " +
                            "LEFT JOIN user_problem_status ups ON ups.problem_id = p.id AND ups.user_id = (SELECT id FROM users WHERE userid = :currentUserId) " +
                            "LEFT JOIN problem_categories pc ON p.id = pc.problem_id " +
                            "LEFT JOIN categories c ON pc.category_id = c.id " +
                            "WHERE p.is_public = true "
            );

            if (query != null && !query.isEmpty()) {
                sql.append("AND p.title LIKE :query ");
            }
            if (category != null && !category.equals("#전체")) {
                sql.append("AND c.tag_name = :category ");
            }

            sql.append("ORDER BY p.id DESC");

            var queryObj = entityManager.createNativeQuery(sql.toString());

            if (query != null && !query.isEmpty()) {
                queryObj.setParameter("query", "%" + query + "%");
            }
            if (category != null && !category.equals("#전체")) {
                queryObj.setParameter("category", category);
            }
            if (currentUserId == null) currentUserId = "";
            queryObj.setParameter("currentUserId", currentUserId);

            List<Object[]> results = queryObj.getResultList();
            Map<Long, Map<String, Object>> problemMap = new LinkedHashMap<>();

            for (Object[] row : results) {
                Long problemId = ((Number) row[0]).longValue();

                if (!problemMap.containsKey(problemId)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", problemId);
                    item.put("title", row[1]);
                    item.put("author", row[2]);
                    item.put("authorId", row[3]);
                    item.put("cardCount", row[4]);
                    item.put("likes", row[5]);
                    item.put("scraps", row[6]);
                    item.put("liked", ((Number) row[7]).intValue() == 1);
                    item.put("scrapped", ((Number) row[8]).intValue() == 1);
                    item.put("categories", new ArrayList<String>());
                    problemMap.put(problemId, item);
                }
                if (row[9] != null) {
                    ((List<String>) problemMap.get(problemId).get("categories")).add(row[9].toString());
                }
            }

            return ResponseEntity.ok(new ArrayList<>(problemMap.values()));
        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // 좋아요 상태 변경 api
    @PostMapping("/api/like/{problemId}")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {

        String userId = (String) data.get("userId");
        Boolean liked = (Boolean) data.get("liked");

        if (userId == null || liked == null || problemId == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "입력값 누락"));
        }

        try {
            // 사용자 내부 ID 조회
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getResultList();

            if (userResult.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "사용자 없음"));
            }

            Long internalUserId = ((Number) userResult.get(0)).longValue();

            // 기존 상태 확인
            List<?> existing = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existing.isEmpty()) {
                // INSERT
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, is_liked) VALUES (?1, ?2, ?3)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, liked ? 1 : 0)
                        .executeUpdate();
            } else {
                // UPDATE
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET is_liked = ?1 WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, liked ? 1 : 0)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }

    // 스크랩 상태 변경 api
    @PostMapping("/api/scrap/{problemId}")
    public ResponseEntity<Map<String, Object>> toggleScrap(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {

        String userId = (String) data.get("userId");
        Boolean scrapped = (Boolean) data.get("scrapped");

        if (userId == null || scrapped == null || problemId == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "입력값 누락"));
        }

        try {
            // 사용자 내부 ID 조회
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getResultList();

            if (userResult.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "사용자 없음"));
            }

            Long internalUserId = ((Number) userResult.get(0)).longValue();

            // 기존 상태 확인
            List<?> existing = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existing.isEmpty()) {
                // INSERT
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, is_scrapped) VALUES (?1, ?2, ?3)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, scrapped ? 1 : 0)
                        .executeUpdate();
            } else {
                // UPDATE
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET is_scrapped = ?1 WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, scrapped ? 1 : 0)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("스크랩 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }
    // 문제 상세 조회 (문제 + 카드 목록)
    @GetMapping("/api/problems/{id}")
    public ResponseEntity<Map<String, Object>> getProblemDetail(
            @PathVariable Long id,
            @RequestParam(required = false) String currentUserId) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, p.description, u.username AS author_name, u.userid AS author_id, p.card_count, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1), 0) AS likes, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_scrapped = 1), 0) AS scraps, " +
                            "IFNULL(ups.is_liked, 0) AS liked, " +
                            "IFNULL(ups.is_scrapped, 0) AS scrapped " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id " +
                            "LEFT JOIN user_problem_status ups ON ups.problem_id = p.id AND ups.user_id = (SELECT id FROM users WHERE userid = :currentUserId) " +
                            "WHERE p.id = :id"
            );

            var queryObj = entityManager.createNativeQuery(sql.toString());
            queryObj.setParameter("id", id);
            if (currentUserId == null) currentUserId = "";
            queryObj.setParameter("currentUserId", currentUserId);

            List<Object[]> problemResults = queryObj.getResultList();
            if (problemResults.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "문제를 찾을 수 없음"));
            }

            Object[] row = problemResults.get(0);
            Map<String, Object> response = new HashMap<>();
            response.put("id", ((Number) row[0]).longValue());
            response.put("title", row[1]);
            response.put("description", row[2]);
            response.put("author", row[3]);
            response.put("authorId", row[4]);
            response.put("cardCount", row[5]);
            response.put("likes", row[6]);
            response.put("scraps", row[7]);
            response.put("liked", ((Number) row[8]).intValue() == 1);
            response.put("scrapped", ((Number) row[9]).intValue() == 1);

            // 카테고리 조회
            List<?> categoryResults = entityManager.createNativeQuery(
                            "SELECT c.tag_name FROM problem_categories pc " +
                                    "JOIN categories c ON pc.category_id = c.id " +
                                    "WHERE pc.problem_id = ?1")
                    .setParameter(1, id)
                    .getResultList();

            List<String> categories = categoryResults.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            response.put("categories", categories);

            // 카드 리스트 조회
            List<?> cardResults = entityManager.createNativeQuery(
                            "SELECT question, correct FROM cards WHERE problem_id = ?1 ORDER BY id ASC")
                    .setParameter(1, id)
                    .getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();
            for (Object result : cardResults) {
                Object[] cardRow = (Object[]) result;
                Map<String, Object> card = new HashMap<>();
                card.put("question", cardRow[0]);
                card.put("correct", cardRow[1]);
                cards.add(card);
            }

            response.put("cards", cards);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("문제 상세 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }



}
