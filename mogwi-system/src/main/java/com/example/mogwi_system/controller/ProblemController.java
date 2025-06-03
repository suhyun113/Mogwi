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
            @RequestParam(required = false) String category
    ) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, u.username AS author_name, u.userid AS author_id, p.card_count, " +
                            "COALESCE((SELECT COUNT(*) FROM likes l WHERE l.problem_id = p.id), 0) AS likes, " +
                            "COALESCE((SELECT COUNT(*) FROM scraps s WHERE s.problem_id = p.id), 0) AS scraps, " +
                            "c1.tag_name AS category1, c2.tag_name AS category2, c3.tag_name AS category3 " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id " +
                            "LEFT JOIN problem_categories pc ON p.id = pc.problem_id " +
                            "LEFT JOIN categories c1 ON pc.category_id_1 = c1.id " +
                            "LEFT JOIN categories c2 ON pc.category_id_2 = c2.id " +
                            "LEFT JOIN categories c3 ON pc.category_id_3 = c3.id " +
                            "WHERE p.is_public = true "
            );

            if (query != null && !query.isEmpty()) {
                sql.append("AND p.title LIKE :query ");
            }
            if (category != null && !category.equals("#전체")) {
                sql.append("AND (:category IN (c1.tag_name, c2.tag_name, c3.tag_name)) ");
            }

            var queryObj = entityManager.createNativeQuery(sql.toString());

            if (query != null && !query.isEmpty()) {
                queryObj.setParameter("query", "%" + query + "%");
            }
            if (category != null && !category.equals("#전체")) {
                queryObj.setParameter("category", category);
            }

            List<Object[]> results = queryObj.getResultList();
            List<Map<String, Object>> problems = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", row[0]);
                item.put("title", row[1]);
                item.put("author", row[2]); // username
                item.put("authorId", row[3]); // userid
                item.put("cardCount", row[4]);
                item.put("likes", row[5]);
                item.put("scraps", row[6]);

                List<String> tags = new ArrayList<>();
                for (int i = 7; i <= 9; i++) {
                    if (row[i] != null) tags.add(row[i].toString());
                }
                item.put("categories", tags);
                item.put("liked", false);  // 초기값: 좋아요 안 한 상태
                item.put("scrapped", false);  // 초기값: 스크랩 안 한 상태
                problems.add(item);
            }

            return ResponseEntity.ok(problems);

        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
}
