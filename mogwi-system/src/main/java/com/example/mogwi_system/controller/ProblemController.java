package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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

    /**
     * 문제 목록 조회 API (사용자 학습 기록 기반 + 필터링)
     * GET /api/problem/detail
     *
     * @param currentUserId 현재 로그인한 사용자의 userid
     * @param onlyLiked true이면 사용자가 좋아요한 문제만 조회
     * @param onlyScrapped true이면 사용자가 스크랩한 문제만 조회
     * @param onlyMine true이면 사용자가 만든 문제만 조회
     * @param onlyPublic true이면 공개된 문제만 조회
     * @return 학습한 문제 목록 (상태, 좋아요, 스크랩, 카드 상태 포함)
     */
    @GetMapping("/api/problem/detail")
    public ResponseEntity<List<Map<String, Object>>> getUserStudyProblemsDetail(
            @RequestParam(required = false) String currentUserId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyLiked,
            @RequestParam(required = false, defaultValue = "false") boolean onlyScrapped,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMine,
            @RequestParam(required = false, defaultValue = "false") boolean onlyPublic
            ) {

        log.info("getUserStudyProblemsDetail 호출: userId={}, onlyLiked={}, onlyScrapped={}, onlyMine={}, onlyPublic={}",
                currentUserId, onlyLiked, onlyScrapped, onlyMine, onlyPublic);

        Long internalUserId = null;

        // currentUserId가 null 이어도 onlyPublic이면 허용
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            try {
                internalUserId = getInternalUserId(currentUserId);
            } catch (NoResultException e) {
                if (!onlyPublic) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("Error getting internal user ID for userId: {}", currentUserId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
            }
        } else if (!onlyPublic && (onlyLiked || onlyScrapped || onlyMine)) {
            // 로그아웃 상태인데 좋아요/스크랩/내 문제 요청 시 에러
            log.warn("currentUserId is required for liked, scrapped, or mine filters when onlyPublic is false.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }

        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id AS problem_id, p.title, p.description, p.is_public, p.card_count, u.username AS author_nickname, u.userid AS author_id," +
                            "COALESCE(ups.is_liked, 0) AS is_liked, COALESCE(ups.is_scrapped, 0) AS is_scrapped, " +
                            "IFNULL(ups.problem_status, '') AS study_status, " +
                            "(SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1) AS total_likes, " +
                            "(SELECT COUNT(*) FROM user_problem_status ups3 WHERE ups3.problem_id = p.id AND ups3.is_scrapped = 1) AS total_scraps, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'perfect' THEN 1 ELSE 0 END), 0) AS perfect_count, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'vague' THEN 1 ELSE 0 END), 0) AS vague_count, " +
                            "COALESCE(SUM(CASE WHEN ucs.card_status = 'forgotten' THEN 1 ELSE 0 END), 0) AS forgotten_count " +
                            "FROM problems p " +
                            "JOIN users u ON p.author_id = u.id "
            );

            if (internalUserId != null) {
                sql.append("LEFT JOIN user_problem_status ups ON ups.problem_id = p.id AND ups.user_id = ").append(internalUserId).append(" ");
                sql.append("LEFT JOIN user_card_status ucs ON ucs.problem_id = p.id AND ucs.user_id = ").append(internalUserId).append(" ");
            } else {
                // 로그아웃 상태에서도 JOIN은 해야 집계가 돌아감 → 가짜 ID로 걸면 안 됨, 그냥 JOIN 없이 null 값 처리되게 해야 함
                sql.append("LEFT JOIN user_problem_status ups ON 1=0 "); // 강제로 항상 null
                sql.append("LEFT JOIN user_card_status ucs ON 1=0 ");    // 강제로 항상 null
            }

            List<String> joinConditions = new ArrayList<>();
            List<Object> parameters = new ArrayList<>();
            List<String> whereConditions = new ArrayList<>();

            if (onlyPublic) {
                whereConditions.add("p.is_public = 1");
            }

            if (onlyMine) {
                if (internalUserId == null) {
                    log.warn("onlyMine=true but currentUserId is null or invalid.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
                }
                whereConditions.add("p.author_id = ?");
                parameters.add(internalUserId);
            }

            if (onlyLiked) {
                whereConditions.add("COALESCE(ups.is_liked, 0) = 1");
            }
            if (onlyScrapped) {
                whereConditions.add("COALESCE(ups.is_scrapped, 0) = 1");
            }

            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }

            sql.append(" GROUP BY p.id, p.title, p.description, p.is_public, p.card_count, u.username, u.userid, ups.is_liked, ups.is_scrapped, ups.problem_status ");
            sql.append(" ORDER BY IFNULL(ups.updated_at, p.created_at) DESC");

            jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString());

            // 파라미터 바인딩
            int paramIndex = 1;
            for (Object param : parameters) {
                query.setParameter(paramIndex++, param);
            }

            // onlyMine 조건이 추가된 경우 internalUserId 바인딩 (parameters 리스트에 이미 추가되지 않은 경우)
            if (onlyMine && internalUserId != null && !parameters.contains(internalUserId)) {
            }

            List<Object[]> problemResults = query.getResultList();

            List<Map<String, Object>> userProblems = new ArrayList<>();

            for (Object[] row : problemResults) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                problem.put("description", row[2]);
                problem.put("isPublic", ((Number) row[3]).intValue() == 1);
                problem.put("cardCount", ((Number) row[4]).intValue());
                problem.put("authorName", row[5]); // author_nickname
                problem.put("authorId", row[6].toString()); // author_id
                problem.put("isLiked", ((Number) row[7]).intValue() == 1);
                problem.put("isScrapped", ((Number) row[8]).intValue() == 1);
                String studyStatus = row[9].toString();
                problem.put("studyStatus", studyStatus);
                problem.put("isCompleted", "completed".equals(studyStatus));
                problem.put("totalLikes", ((Number) row[10]).intValue());
                problem.put("totalScraps", ((Number) row[11]).intValue());
                problem.put("perfectCount", ((Number) row[12]).intValue());
                problem.put("vagueCount", ((Number) row[13]).intValue());
                problem.put("forgottenCount", ((Number) row[14]).intValue());
                problem.put("categories", getCategoriesForProblem(problemId));

                userProblems.add(problem);
            }

            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("사용자 문제 상세 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /** 단일 문제 조회 API
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
    @DeleteMapping("/api/problem/{problemId}")
    public ResponseEntity<Map<String, String>> deleteProblemStatus(
            @PathVariable Long problemId,
            @PathVariable String userId) {
        log.warn("MyStudyController - deleteProblemStatus 호출됨: problemId={}, userId={}. !!! 전체 문제 데이터가 삭제됩니다 !!!", problemId, userId);
        Map<String, String> response = new HashMap<>();

        if (userId == null || userId.trim().isEmpty() || problemId == null) {
            log.warn("MyStudyController - deleteProblemStatus: 필수 입력값 누락. problemId={}, userId={}", problemId, userId);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, problemId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // internalUserId is still needed for logging, but not strictly for deleting ALL statuses
        Long internalUserId = null;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - deleteProblemStatus: 사용자 ID '{}'를 찾을 수 없음. 하지만 문제 전체 삭제는 진행됩니다.", userId);
            // We might still proceed with problem deletion even if user is not found,
            // as the intent here is global deletion tied to this endpoint.
            // Consider if you want to allow this or always require a valid user.
        } catch (Exception e) {
            log.error("MyStudyController - deleteProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            // No rollback here yet, as we haven't started DB operations
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // Start Transaction (if not already handled by Spring's @Transactional)
            // It's highly recommended to use @Transactional on the service layer method
            // that orchestrates these deletes, rather than directly in the controller.

            // 1. Delete ALL user_card_status entries for this problem (for ALL users)
            String deleteGlobalCardStatusSql = "DELETE FROM user_card_status WHERE problem_id = ?1";
            int deletedGlobalCardStatusCount = entityManager.createNativeQuery(deleteGlobalCardStatusSql)
                    .setParameter(1, problemId)
                    .executeUpdate();
            log.info("MyStudyController - deleted {} user_card_status entries for problemId={}", deletedGlobalCardStatusCount, problemId);

            // 2. Delete ALL user_problem_status entries for this problem (for ALL users)
            String deleteGlobalProblemStatusSql = "DELETE FROM user_problem_status WHERE problem_id = ?1";
            int deletedGlobalProblemStatusCount = entityManager.createNativeQuery(deleteGlobalProblemStatusSql)
                    .setParameter(1, problemId)
                    .executeUpdate();
            log.info("MyStudyController - deleted {} user_problem_status entries for problemId={}", deletedGlobalProblemStatusCount, problemId);

            // 3. Delete cards associated with this problem
            String deleteCardsSql = "DELETE FROM cards WHERE problem_id = ?1";
            int deletedCardsCount = entityManager.createNativeQuery(deleteCardsSql)
                    .setParameter(1, problemId)
                    .executeUpdate();
            log.info("MyStudyController - deleted {} cards for problemId={}", deletedCardsCount, problemId);

            // 4. Delete problem_categories entries related to this problem
            //    (Assuming problem_categories is a join table or has problem_id directly)
            String deleteProblemCategoriesSql = "DELETE FROM problem_categories WHERE problem_id = ?1";
            int deletedCategoriesCount = entityManager.createNativeQuery(deleteProblemCategoriesSql)
                    .setParameter(1, problemId)
                    .executeUpdate();
            log.info("MyStudyController - deleted {} problem_categories entries for problemId={}", deletedCategoriesCount, problemId);

            // 5. Finally, delete the problem itself
            String deleteProblemSql = "DELETE FROM problems WHERE id = ?1";
            int deletedProblemCount = entityManager.createNativeQuery(deleteProblemSql)
                    .setParameter(1, problemId)
                    .executeUpdate();
            log.info("MyStudyController - deleted {} problem from 'problems' table for problemId={}", deletedProblemCount, problemId);


            if (deletedProblemCount > 0) {
                response.put("status", "OK");
                response.put("message", "문제 및 모든 관련 데이터가 시스템에서 성공적으로 삭제되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "INFO"); // Not an error, but problem was not found for deletion
                response.put("message", "해당 문제(ID: " + problemId + ")를 찾을 수 없거나 이미 시스템에서 삭제되었습니다.");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } catch (Exception e) {
            log.error("MyStudyController - deleteProblemStatus: 문제 및 관련 데이터 삭제 중 오류 발생 (problemId: {}): {}", problemId, e.getMessage(), e);
            // This is critical, ensure rollback if using programmatic transaction management
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}