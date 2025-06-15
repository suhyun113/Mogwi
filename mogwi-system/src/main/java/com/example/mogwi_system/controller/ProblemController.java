package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Transactional
public class ProblemController {

    @PersistenceContext
    private EntityManager entityManager;

    // --- 문제 목록 조회 API (color_code 추가) ---
    @GetMapping("/api/problems")
    public ResponseEntity<List<Map<String, Object>>> getProblems(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String currentUserId
    ) {
        try {
            // SQL 쿼리에 c.color_code 추가
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, u.username AS author_name, u.userid AS author_id, p.card_count, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1), 0) AS likes, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_scrapped = 1), 0) AS scraps, " +
                            "IFNULL(ups.is_liked, 0) AS liked, " +
                            "IFNULL(ups.is_scrapped, 0) AS scrapped, " +
                            "c.tag_name AS category_name, " + // category_name으로 컬럼명 변경
                            "c.color_code AS category_color " + // category_color 컬럼 추가
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

            // GROUP BY 절에 category_name, category_color 추가
            sql.append("GROUP BY p.id, p.title, u.username, u.userid, p.card_count, ups.is_liked, ups.is_scrapped, category_name, category_color ");
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
                    item.put("categories", new ArrayList<Map<String, String>>()); // List<Map<String, String>>으로 변경
                    problemMap.put(problemId, item);
                }
                // row[9]는 tag_name, row[10]은 color_code
                if (row[9] != null) {
                    Map<String, String> categoryMap = new HashMap<>();
                    categoryMap.put("tag_name", row[9].toString());
                    categoryMap.put("color_code", row[10] != null ? row[10].toString() : "#CCCCCC"); // null 처리 및 기본값
                    ((List<Map<String, String>>) problemMap.get(problemId).get("categories")).add(categoryMap);
                }
            }

            return ResponseEntity.ok(new ArrayList<>(problemMap.values()));
        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    // --- 기존 좋아요 상태 변경 API (변경 없음) ---
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
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getResultList();

            if (userResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자 없음"));
            }

            Long internalUserId = ((Number) userResult.get(0)).longValue();

            List<?> existing = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existing.isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, is_liked) VALUES (?1, ?2, ?3)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, liked ? 1 : 0)
                        .executeUpdate();
            } else {
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET is_liked = ?1 WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, liked ? 1 : 0)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }

    // --- 기존 스크랩 상태 변경 API (변경 없음) ---
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
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getResultList();

            if (userResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "사용자 없음"));
            }

            Long internalUserId = ((Number) userResult.get(0)).longValue();

            List<?> existing = entityManager.createNativeQuery(
                            "SELECT id FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2")
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getResultList();

            if (existing.isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO user_problem_status (user_id, problem_id, is_scrapped) VALUES (?1, ?2, ?3)")
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, scrapped ? 1 : 0)
                        .executeUpdate();
            } else {
                entityManager.createNativeQuery(
                                "UPDATE user_problem_status SET is_scrapped = ?1 WHERE user_id = ?2 AND problem_id = ?3")
                        .setParameter(1, scrapped ? 1 : 0)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("스크랩 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }

    // --- 문제 상세 조회 API (color_code 추가) ---
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "ERROR", "message", "문제를 찾을 수 없음"));
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

            // 카테고리 조회 (tag_name과 color_code 포함)
            List<?> categoryResults = entityManager.createNativeQuery(
                            "SELECT c.tag_name, c.color_code FROM problem_categories pc " +
                                    "JOIN categories c ON pc.category_id = c.id " +
                                    "WHERE pc.problem_id = ?1")
                    .setParameter(1, id)
                    .getResultList();

            // List<Map<String, String>> 형태로 변환
            List<Map<String, String>> categoriesWithColor = new ArrayList<>();
            for (Object result : categoryResults) {
                Object[] categoryRow = (Object[]) result;
                Map<String, String> categoryMap = new HashMap<>();
                categoryMap.put("tag_name", categoryRow[0].toString());
                categoryMap.put("color_code", categoryRow[1] != null ? categoryRow[1].toString() : "#CCCCCC"); // null 처리 및 기본값
                categoriesWithColor.add(categoryMap);
            }
            response.put("categories", categoriesWithColor);

            // 카드 리스트 조회 (정답 컬럼은 'correct'임, 'answer' 아님)
            List<?> cardResults = entityManager.createNativeQuery(
                            "SELECT question, correct, image_url FROM cards WHERE problem_id = ?1 ORDER BY id ASC")
                    .setParameter(1, id)
                    .getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();
            for (Object result : cardResults) {
                Object[] cardRow = (Object[]) result;
                Map<String, Object> card = new HashMap<>();
                card.put("question", cardRow[0]);
                card.put("answer", cardRow[1]);
                card.put("image_url", cardRow[2]);
                cards.add(card);
            }

            response.put("cards", cards);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("문제 상세 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "서버 오류"));
        }
    }

    // --- 새로운 API: 카테고리 목록 조회 (color_code 추가) ---
    @GetMapping("/api/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllCategories() {
        try {
            // SQL 쿼리에 color_code 컬럼 추가
            List<?> results = entityManager.createNativeQuery("SELECT id, tag_name, color_code FROM categories ORDER BY tag_name ASC")
                    .getResultList();

            List<Map<String, Object>> categories = new ArrayList<>();
            for (Object result : results) {
                Object[] row = (Object[]) result;
                Map<String, Object> categoryMap = new HashMap<>();
                categoryMap.put("id", ((Number) row[0]).longValue());
                categoryMap.put("tag_name", row[1].toString());
                // color_code 추가
                categoryMap.put("color_code", row[2] != null ? row[2].toString() : "#CCCCCC"); // null 처리 및 기본값 설정
                categories.add(categoryMap);
            }
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // --- 새로운 API: 문제 생성 (변경 없음) ---
    @PostMapping("/api/problems")
    public ResponseEntity<Map<String, String>> createProblem(@RequestBody Map<String, Object> requestBody) {
        Map<String, String> response = new HashMap<>();
        try {
            // 1. 요청 데이터 파싱 및 기본 유효성 검사
            String title = (String) requestBody.get("title");
            String authorIdString = (String) requestBody.get("author_id");
            String description = (String) requestBody.get("description");

            Integer isPublicInt = (Integer) requestBody.get("is_public");
            boolean isPublic = (isPublicInt != null && isPublicInt == 1);


            @SuppressWarnings("unchecked")
            List<Integer> categoryIds = (List<Integer>) requestBody.get("categories");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) requestBody.get("cards");

            // --- 유효성 검사 로직 (변경 없음) ---
            if (title == null || title.trim().isEmpty()) {
                response.put("status", "FAIL");
                response.put("message", "문제 제목은 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            if (authorIdString == null || authorIdString.trim().isEmpty()) {
                response.put("status", "FAIL");
                response.put("message", "작성자 정보가 누락되었습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            if (categoryIds == null || categoryIds.isEmpty()) {
                response.put("status", "FAIL");
                response.put("message", "태그는 최소 1개 이상 선택해야 합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            if (categoryIds.size() > 3) {
                response.put("status", "FAIL");
                response.put("message", "태그는 최대 3개까지 선택할 수 있습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            if (cards == null || cards.isEmpty()) {
                response.put("status", "FAIL");
                response.put("message", "카드는 최소 1개 이상 추가해야 합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            for (Map<String, Object> card : cards) {
                String question = (String) card.get("question");
                String answer = (String) card.get("answer");
                if (question == null || question.trim().isEmpty() || answer == null || answer.trim().isEmpty()) {
                    response.put("status", "FAIL");
                    response.put("message", "모든 카드에 질문과 정답을 입력해주세요.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            // --- 유효성 검사 로직 끝 ---

            // 2. Vue의 author_id (userid 문자열)를 실제 users 테이블의 id (Long)로 변환
            Long authorInternalId;
            List<?> userResult = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, authorIdString)
                    .getResultList();

            if (userResult.isEmpty()) {
                response.put("status", "FAIL");
                response.put("message", "존재하지 않는 사용자입니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            authorInternalId = ((Number) userResult.get(0)).longValue();

            // 3. problems 테이블에 문제 삽입
            String insertProblemSql = "INSERT INTO problems (title, description, author_id, card_count, is_public) VALUES (?1, ?2, ?3, ?4, ?5)";
            entityManager.createNativeQuery(insertProblemSql)
                    .setParameter(1, title)
                    .setParameter(2, description != null && !description.isEmpty() ? description : null)
                    .setParameter(3, authorInternalId)
                    .setParameter(4, cards.size())
                    .setParameter(5, isPublic ? 1 : 0)
                    .executeUpdate();

            // 삽입된 문제의 ID 조회 (auto_increment 된 ID)
            // Long으로 직접 캐스팅하거나 Number로 받은 후 longValue()를 사용
            // 기존: BigInteger problemIdBigInt = (BigInteger) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
            Number lastInsertId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
            Long problemId = lastInsertId.longValue(); // Number에서 longValue()를 호출하여 안전하게 Long으로 변환


            // 4. problem_categories 테이블에 카테고리 연결
            String insertProblemCategorySql = "INSERT INTO problem_categories (problem_id, category_id) VALUES (?1, ?2)";
            for (Integer categoryId : categoryIds) {
                entityManager.createNativeQuery(insertProblemCategorySql)
                        .setParameter(1, problemId)
                        .setParameter(2, categoryId.longValue())
                        .executeUpdate();
            }

            // 5. cards 테이블에 학습 카드 삽입
            String insertCardSql = "INSERT INTO cards (problem_id, question, correct, image_url) VALUES (?1, ?2, ?3, ?4)";
            for (Map<String, Object> card : cards) {
                String question = (String) card.get("question");
                String correct = (String) card.get("answer");
                String imageUrl = (String) card.get("image_url");

                entityManager.createNativeQuery(insertCardSql)
                        .setParameter(1, problemId)
                        .setParameter(2, question)
                        .setParameter(3, correct)
                        .setParameter(4, imageUrl != null && !imageUrl.isEmpty() ? imageUrl : null)
                        .executeUpdate();
            }

            response.put("status", "OK");
            response.put("message", "문제가 성공적으로 생성되었습니다.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ClassCastException e) {
            log.error("문제 생성 중 데이터 타입 캐스팅 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "전달된 데이터의 형식이 올바르지 않습니다. (예: 공개 여부, 숫자 캐스팅 오류)");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("문제 생성 중 서버 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "문제 생성 중 서버 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}