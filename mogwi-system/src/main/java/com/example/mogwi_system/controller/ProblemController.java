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
@Slf4j
@Transactional
public class ProblemController {

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

    // Helper method: 문제의 카테고리 태그와 색상 코드 조회
    private List<Map<String, String>> getCategoriesForProblem(Long problemId) {
        List<Object[]> results = entityManager.createNativeQuery(
                        "SELECT c.tag_name, c.color_code " +
                                "FROM problem_categories pc " +
                                "JOIN categories c ON pc.category_id = c.id " +
                                "WHERE pc.problem_id = ?1")
                .setParameter(1, problemId)
                .getResultList();

        List<Map<String, String>> categories = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, String> category = new HashMap<>();
            category.put("tag_name", row[0].toString());
            category.put("color_code", row[1] != null ? row[1].toString() : "#CCCCCC");
            categories.add(category);
        }
        return categories;
    }

    /** 문제 목록 조회 API
     * GET /api/problem
     *
     * @param query 검색어 (선택 사항)
     * @param category 카테고리 태그 (선택 사항)
     * @param currentUserId 현재 로그인된 사용자의 userid (문자열). 이 사용자의 좋아요/스크랩 여부 및 (onlyMine=true 시) 작성 문제를 확인하는 데 사용됩니다.
     * @return 문제 목록 (List<Map<String, Object>>)
     */
    @GetMapping("/api/problem")
    public ResponseEntity<List<Map<String, Object>>> getProblems(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String currentUserId
    ) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.title, u.username AS author_name, u.userid AS author_id, p.is_public, p.card_count, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1), 0) AS likes, " +
                            "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_scrapped = 1), 0) AS scraps, " +
                            "IFNULL(ups.is_liked, 0) AS liked, " +
                            "IFNULL(ups.is_scrapped, 0) AS scrapped, " +
                            "c.tag_name AS category_name, " +
                            "c.color_code AS category_color " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id " +
                            "LEFT JOIN user_problem_status ups ON ups.problem_id = p.id AND ups.user_id = (SELECT id FROM users WHERE userid = :currentUserIdParam) " + // currentUserIdParam으로 변경
                            "LEFT JOIN problem_categories pc ON p.id = pc.problem_id " +
                            "LEFT JOIN categories c ON pc.category_id = c.id "
            );

            List<String> conditions = new ArrayList<>();
            Map<String, Object> parameters = new HashMap<>();

            // 검색어 조건
            if (query != null && !query.isEmpty()) {
                conditions.add("p.title LIKE :query");
                parameters.put("query", "%" + query + "%");
            }
            // 카테고리 조건
            if (category != null && !category.equals("#전체")) {
                conditions.add("c.tag_name = :category");
                parameters.put("category", category);
            }

            // 조건들을 WHERE 절에 추가
            if (!conditions.isEmpty()) {
                sql.append("WHERE ").append(String.join(" AND ", conditions));
                sql.append(" AND p.is_public = 1");
            } else {
                sql.append("WHERE p.is_public = 1");
            }

            // GROUP BY 및 ORDER BY 절
            sql.append(" GROUP BY p.id, p.title, u.username, u.userid, p.is_public, p.card_count, ups.is_liked, ups.is_scrapped, category_name, category_color ");
            sql.append(" ORDER BY p.id DESC");

            var queryObj = entityManager.createNativeQuery(sql.toString());

            // 쿼리 파라미터 설정
            if (currentUserId == null) currentUserId = ""; // currentUserId가 null이면 빈 문자열로 처리
            queryObj.setParameter("currentUserIdParam", currentUserId); // 좋아요/스크랩 상태 조회용 파라미터

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                queryObj.setParameter(entry.getKey(), entry.getValue());
            }

            List<Object[]> results = queryObj.getResultList();
            Map<Long, Map<String, Object>> problemMap = new LinkedHashMap<>();

            for (Object[] row : results) {
                Long problemId = ((Number) row[0]).longValue();

                if (!problemMap.containsKey(problemId)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", problemId);
                    item.put("title", row[1]);
                    item.put("authorName", row[2]); // username
                    item.put("authorId", row[3]); // userId
                    item.put("isPublic","1".equals(row[4].toString()));
                    item.put("cardCount", row[5]);
                    item.put("likes", row[6]);
                    item.put("scraps", row[7]);
                    item.put("liked", ((Number) row[8]).intValue() == 1);
                    item.put("scrapped", ((Number) row[9]).intValue() == 1);
                    item.put("categories", new ArrayList<Map<String, String>>());
                    problemMap.put(problemId, item);
                }
                if (row[10] != null) {
                    Map<String, String> categoryMap = new HashMap<>();
                    categoryMap.put("tag_name", row[10].toString());
                    categoryMap.put("color_code", row[11] != null ? row[11].toString() : "#CCCCCC");
                    ((List<Map<String, String>>) problemMap.get(problemId).get("categories")).add(categoryMap);
                }
            }

            return ResponseEntity.ok(new ArrayList<>(problemMap.values()));
        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 상세 문제 목록 조회 API (사용자 학습 기록 기반 + 필터링)
     * GET /api/problem/detail
     *
     * @param currentUserId 현재 로그인한 사용자의 userid
     * @param onlyLiked true이면 사용자가 좋아요한 문제만 조회
     * @param onlyScrapped true이면 사용자가 스크랩한 문제만 조회
     * @param onlyMine true이면 사용자가 만든 문제만 조회
     * @return 학습한 문제 목록 (상태, 좋아요, 스크랩, 카드 상태 포함)
     */
    @GetMapping("/api/problem/detail")
    public ResponseEntity<List<Map<String, Object>>> getUserStudyProblemsDetail(
            @RequestParam String currentUserId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyLiked,
            @RequestParam(required = false, defaultValue = "false") boolean onlyScrapped,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMine) {

        log.info("getUserStudyProblemsDetail 호출: userId={}", currentUserId);
        Long internalUserId;
        try {
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(currentUserId);
        } catch (NoResultException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id AS problem_id, p.title, p.description, p.is_public, p.card_count, u.username AS author_nickname, " +
                            "COALESCE(ups.is_liked, 0) AS is_liked, COALESCE(ups.is_scrapped, 0) AS is_scrapped, " +
                            "IFNULL(ups.problem_status, 'new') AS study_status, " +
                            "(SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1) AS total_likes, " +
                            "(SELECT COUNT(*) FROM user_problem_status ups3 WHERE ups3.problem_id = p.id AND ups3.is_scrapped = 1) AS total_scraps, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' THEN 1 ELSE 0 END), 0) AS perfect_count, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' THEN 1 ELSE 0 END), 0) AS vague_count, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' THEN 1 ELSE 0 END), 0) AS forgotten_count " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id " +
                            "LEFT JOIN user_problem_status ups ON p.id = ups.problem_id AND ups.user_id = ?1 " +
                            "LEFT JOIN user_card_status ucs ON p.id = ucs.problem_id AND ucs.user_id = ?1 "
            );

            List<String> whereConditions = new ArrayList<>();
            if (onlyMine) {
                whereConditions.add("p.author_id = ?1");
            } else {
                whereConditions.add("(ups.user_id = ?1 OR ucs.user_id = ?1)");
            }

            if (onlyLiked) {
                whereConditions.add("ups.is_liked = 1");
            }
            if (onlyScrapped) {
                whereConditions.add("ups.is_scrapped = 1");
            }

            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }

            sql.append(" GROUP BY p.id, p.title, p.description, p.card_count, u.username, ups.is_liked, ups.is_scrapped, ups.problem_status ");
            sql.append(" ORDER BY IFNULL(ups.updated_at, p.created_at) DESC");

            List<Object[]> problemResults = entityManager.createNativeQuery(sql.toString())
                    .setParameter(1, internalUserId)
                    .getResultList();

            List<Map<String, Object>> userProblems = new ArrayList<>();

            for (Object[] row : problemResults) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                problem.put("description", row[2]);
                problem.put("isPublic", ((Number) row[3]).intValue() == 1);
                problem.put("cardCount", ((Number) row[4]).intValue());
                problem.put("authorName", row[5]); // userName
                problem.put("isLiked", ((Number) row[6]).intValue() == 1);
                problem.put("isScrapped", ((Number) row[7]).intValue() == 1);
                String studyStatus = row[8].toString();
                problem.put("studyStatus", studyStatus);
                problem.put("isCompleted", "completed".equals(studyStatus));
                problem.put("totalLikes", ((Number) row[9]).intValue());
                problem.put("totalScraps", ((Number) row[10]).intValue());
                problem.put("perfectCount", ((Number) row[11]).intValue());
                problem.put("vagueCount", ((Number) row[12]).intValue());
                problem.put("forgottenCount", ((Number) row[13]).intValue());
                problem.put("categories", getCategoriesForProblem(problemId));

                userProblems.add(problem);
            }

            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("사용자 문제 상세 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /** 단일 문제 상세 조회 API
     * GET /api/problem/{id}
     */
    @GetMapping("/api/problem/{id}")
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
            response.put("authorName", row[3]); // userName
            response.put("authorId", row[4]); // userId
            response.put("cardCount", row[5]);
            response.put("likes", row[6]);
            response.put("scraps", row[7]);
            response.put("liked", ((Number) row[8]).intValue() == 1);
            response.put("scrapped", ((Number) row[9]).intValue() == 1);

            List<?> categoryResults = entityManager.createNativeQuery(
                            "SELECT c.tag_name, c.color_code FROM problem_categories pc " +
                                    "JOIN categories c ON pc.category_id = c.id " +
                                    "WHERE pc.problem_id = ?1")
                    .setParameter(1, id)
                    .getResultList();

            List<Map<String, String>> categoriesWithColor = new ArrayList<>();
            for (Object result : categoryResults) {
                Object[] categoryRow = (Object[]) result;
                Map<String, String> categoryMap = new HashMap<>();
                categoryMap.put("tag_name", categoryRow[0].toString());
                categoryMap.put("color_code", categoryRow[1] != null ? categoryRow[1].toString() : "#CCCCCC");
                categoriesWithColor.add(categoryMap);
            }
            response.put("categories", categoriesWithColor);

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

    // 카테고리 목록 조회 API
    @GetMapping("/api/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllCategories() {
        try {
            List<?> results = entityManager.createNativeQuery("SELECT id, tag_name, color_code FROM categories ORDER BY tag_name ASC")
                    .getResultList();

            List<Map<String, Object>> categories = new ArrayList<>();
            for (Object result : results) {
                Object[] row = (Object[]) result;
                Map<String, Object> categoryMap = new HashMap<>();
                categoryMap.put("id", ((Number) row[0]).longValue());
                categoryMap.put("tag_name", row[1].toString());
                categoryMap.put("color_code", row[2] != null ? row[2].toString() : "#CCCCCC");
                categories.add(categoryMap);
            }
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /** 문제 생성 API
     * POST /api/problem
     */
    @PostMapping("/api/problem")
    public ResponseEntity<Map<String, String>> createProblem(@RequestBody Map<String, Object> requestBody) {
        Map<String, String> response = new HashMap<>();
        try {
            String title = (String) requestBody.get("title");
            String authorIdString = (String) requestBody.get("author_id");
            String description = (String) requestBody.get("description");

            Integer isPublicInt = (Integer) requestBody.get("is_public");
            boolean isPublic = (isPublicInt != null && isPublicInt == 1);


            @SuppressWarnings("unchecked")
            List<Integer> categoryIds = (List<Integer>) requestBody.get("categories");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) requestBody.get("cards");

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

            String insertProblemSql = "INSERT INTO problems (title, description, author_id, card_count, is_public) VALUES (?1, ?2, ?3, ?4, ?5)";
            entityManager.createNativeQuery(insertProblemSql)
                    .setParameter(1, title)
                    .setParameter(2, description != null && !description.isEmpty() ? description : null)
                    .setParameter(3, authorInternalId)
                    .setParameter(4, cards.size())
                    .setParameter(5, isPublic ? 1 : 0)
                    .executeUpdate();

            Number lastInsertId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
            Long problemId = lastInsertId.longValue();

            String insertProblemCategorySql = "INSERT INTO problem_categories (problem_id, category_id) VALUES (?1, ?2)";
            for (Integer categoryId : categoryIds) {
                entityManager.createNativeQuery(insertProblemCategorySql)
                        .setParameter(1, problemId)
                        .setParameter(2, categoryId.longValue())
                        .executeUpdate();
            }

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


    /** 문제 수정 API
     * PUT /api/problem/{id}
     */
    @PutMapping("/api/problem/{id}")
    public ResponseEntity<Map<String, String>> updateProblem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {

        Map<String, String> response = new HashMap<>();
        try {
            String title = (String) requestBody.get("title");
            String description = (String) requestBody.get("description");
            Integer isPublicInt = (Integer) requestBody.get("is_public");
            boolean isPublic = (isPublicInt != null && isPublicInt == 1);

            @SuppressWarnings("unchecked")
            List<Integer> categoryIds = (List<Integer>) requestBody.get("categories");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) requestBody.get("cards");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "제목은 필수입니다."));
            }
            if (categoryIds == null || categoryIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "태그는 1개 이상 필요합니다."));
            }
            if (cards == null || cards.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "카드는 최소 1개 이상이어야 합니다."));
            }

            for (Map<String, Object> card : cards) {
                String question = (String) card.get("question");
                String answer = (String) card.get("answer");
                if (question == null || question.trim().isEmpty() || answer == null || answer.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "모든 카드에 질문과 정답을 입력해주세요."));
                }
            }

            String updateSql = "UPDATE problems SET title = ?1, description = ?2, card_count = ?3, is_public = ?4 WHERE id = ?5";
            entityManager.createNativeQuery(updateSql)
                    .setParameter(1, title)
                    .setParameter(2, description != null && !description.isEmpty() ? description : null)
                    .setParameter(3, cards.size())
                    .setParameter(4, isPublic ? 1 : 0)
                    .setParameter(5, id)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM problem_categories WHERE problem_id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            for (Integer categoryId : categoryIds) {
                entityManager.createNativeQuery("INSERT INTO problem_categories (problem_id, category_id) VALUES (?1, ?2)")
                        .setParameter(1, id)
                        .setParameter(2, categoryId.longValue())
                        .executeUpdate();
            }

            entityManager.createNativeQuery("DELETE FROM cards WHERE problem_id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            for (Map<String, Object> card : cards) {
                String question = (String) card.get("question");
                String correct = (String) card.get("answer");
                String imageUrl = (String) card.get("image_url");

                entityManager.createNativeQuery(
                                "INSERT INTO cards (problem_id, question, correct, image_url) VALUES (?1, ?2, ?3, ?4)")
                        .setParameter(1, id)
                        .setParameter(2, question)
                        .setParameter(3, correct)
                        .setParameter(4, imageUrl != null && !imageUrl.isEmpty() ? imageUrl : null)
                        .executeUpdate();
            }

            return ResponseEntity.ok(Map.of("status", "OK", "message", "문제가 성공적으로 수정되었습니다."));

        } catch (Exception e) {
            log.error("문제 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", "문제 수정 중 오류 발생"));
        }
    }

    /** 문제 삭제 API
     * DELETE /api/problem/{id}
     */
    @DeleteMapping("/api/problem/{id}")
    public ResponseEntity<Map<String, String>> deleteProblem(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            Long problemCount = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM problems WHERE id = ?1")
                    .setParameter(1, id)
                    .getSingleResult()).longValue();

            if (problemCount == 0) {
                response.put("status", "FAIL");
                response.put("message", "삭제할 문제를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            entityManager.createNativeQuery("DELETE FROM user_problem_status WHERE problem_id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM problem_categories WHERE problem_id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM cards WHERE problem_id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            int deletedRows = entityManager.createNativeQuery("DELETE FROM problems WHERE id = ?1")
                    .setParameter(1, id)
                    .executeUpdate();

            if (deletedRows > 0) {
                response.put("status", "OK");
                response.put("message", "문제가 성공적으로 삭제되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "FAIL");
                response.put("message", "문제 삭제에 실패했습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            log.error("문제 삭제 중 오류 발생: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "문제 삭제 중 서버 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}