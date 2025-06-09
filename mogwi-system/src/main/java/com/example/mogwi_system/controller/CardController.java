// src/main/java/com/example/mogwi_system/controller/CardController.java

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
@RequestMapping("/api") // 모든 엔드포인트의 기본 경로를 /api로 설정
public class CardController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 특정 문제(problemId)에 속한 카드 목록과 해당 사용자의 학습 상태를 조회합니다.
     * GET /api/problems/{problemId}/cards
     *
     * @param problemId 조회할 문제의 ID
     * @param currentUserId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 문제 카드 목록과 각 카드의 학습 상태를 포함하는 응답
     */
    @GetMapping("/study/{problemId}/cards")
    public ResponseEntity<List<Map<String, Object>>> getCardsForProblem(
            @PathVariable Long problemId,
            @RequestParam(required = false) String currentUserId
    ) {
        try {
            String effectiveCurrentUserId = (currentUserId != null) ? currentUserId : "";

            StringBuilder sql = new StringBuilder(
                    "SELECT c.id, c.question, c.correct, c.image_url, " + // ✅ image_url 추가
                            "IFNULL(ucs.card_status, 'new') AS card_status " +
                            "FROM cards c " +
                            "LEFT JOIN user_card_status ucs ON c.id = ucs.card_id AND ucs.problem_id = :problemId " +
                            "AND ucs.user_id = (SELECT id FROM users WHERE userid = :currentUserId) " +
                            "WHERE c.problem_id = :problemId " +
                            "ORDER BY c.id ASC"
            );

            var queryObj = entityManager.createNativeQuery(sql.toString());
            queryObj.setParameter("problemId", problemId);
            queryObj.setParameter("currentUserId", effectiveCurrentUserId);

            List<Object[]> results = queryObj.getResultList();
            List<Map<String, Object>> cards = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("id", ((Number) row[0]).longValue());
                card.put("question", row[1]);
                card.put("answer", row[2]);
                card.put("imageUrl", row[3]); // ✅ 프론트엔드로 이미지 URL 전달
                card.put("cardStatus", row[4].toString());
                cards.add(card);
            }

            if (cards.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("문제 카드 및 학습 상태 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}