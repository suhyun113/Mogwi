package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class ProblemController {

    @PersistenceContext
    private EntityManager entityManager;

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
            if (currentUserId == null) {
                currentUserId = ""; // NULL 대비
            }
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
}
