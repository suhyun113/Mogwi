package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
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

    // Helper method: 문제의 카테고리 태그와 색상 코드 조회
    private List<Map<String, String>> getCategoriesForProblem(Long problemId) {
        List<Map<String, String>> categoriesWithColor = new ArrayList<>();
        try {
            String tagsSql = "SELECT c.tag_name, c.color_code FROM categories c " +
                    "JOIN problem_categories pc ON c.id = pc.category_id " +
                    "WHERE pc.problem_id = ?1";
            List<Object[]> tagsAndColors = entityManager.createNativeQuery(tagsSql)
                    .setParameter(1, problemId)
                    .getResultList();

            for (Object[] tagRow : tagsAndColors) {
                Map<String, String> categoryMap = new HashMap<>();
                categoryMap.put("tag_name", tagRow[0].toString());
                categoryMap.put("color_code", tagRow[1] != null ? tagRow[1].toString() : "#CCCCCC"); // null 처리 및 기본값 설정
                categoriesWithColor.add(categoryMap);
            }
        } catch (Exception e) {
            log.error("MyStudyController: 문제 ID {}에 대한 카테고리 조회 중 오류 발생: {}", problemId, e.getMessage(), e);
            // 오류 발생 시에도 빈 리스트 반환 (클라이언트에서 처리할 수 있도록)
        }
        return categoriesWithColor;
    }

    /**
     * 특정 사용자의 전체 학습 요약 통계를 조회합니다.
     * GET /api/mystudy/summary/{userId}
     * (예: 완벽, 모호, 망각 카운트)
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 전체 학습 요약 데이터
     */
    @GetMapping("/problem/{userId}")
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
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 지정
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
            // 읽기 전용 트랜잭션에서는 롤백이 의미 없거나 자동이므로 명시적 호출 불필요
            // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 이 라인은 읽기 전용에서는 보통 제거
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            // 핵심 수정: p.id IN (...) 서브쿼리를 제거하고, LEFT JOIN 후 WHERE 절에서
            // 해당 사용자의 user_problem_status 또는 user_card_status 기록이 있는 문제만 필터링합니다.
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
                    "LEFT JOIN user_card_status ucs ON p.id = ucs.problem_id AND ucs.user_id = ?1 " +
                    // 변경된 부분: 현재 사용자의 학습 기록(문제 상태 또는 카드 상태)이 있는 문제만 가져옴
                    "WHERE (ups.user_id = ?1 OR ucs.user_id = ?1) " +
                    "GROUP BY p.id, p.title, p.description, p.card_count, u.username, ups.is_liked, ups.is_scrapped, ups.problem_status " +
                    "ORDER BY IFNULL(ups.updated_at, p.created_at) DESC";

            // named parameter로 변경하여 가독성 높임
            List<Object[]> problemResults = entityManager.createNativeQuery(problemSql)
                    .setParameter(1, internalUserId) // 기존 ?1, ?2, ?3 대신 하나의 파라미터로 처리
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

                // 카테고리 태그와 색상 코드 조회 (헬퍼 메소드 사용)
                problem.put("categories", getCategoriesForProblem(problemId));

                userProblems.add(problem);
            }
            log.info("MyStudyController - getUserStudyProblemsDetail 성공: userId={}에 대해 {}개의 문제 조회됨.", userId, userProblems.size());
            return ResponseEntity.ok(userProblems);

        } catch (Exception e) {
            log.error("MyStudyController - getUserStudyProblemsDetail: 사용자 문제 학습 상세 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            // 읽기 전용 트랜잭션에서 롤백은 자동이지만, 명시적으로 표시해도 무방
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * 특정 사용자가 스크랩한 문제 목록을 조회합니다.
     * GET /api/mystudy/scrapped/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 스크랩된 문제 목록
     */
    @GetMapping("/scrapped/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getScrappedProblems(
            @PathVariable String userId) {
        log.info("MyStudyController - getScrappedProblems 호출됨: userId={}", userId);
        Long internalUserId;
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("MyStudyController - getScrappedProblems: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - getScrappedProblems: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("MyStudyController - getScrappedProblems: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            String sql = "SELECT " +
                    "p.id, p.title, u.username AS author_name, u.userid AS author_id, p.card_count, " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_liked = 1), 0) AS likes, " +
                    "COALESCE((SELECT COUNT(*) FROM user_problem_status ups2 WHERE ups2.problem_id = p.id AND ups2.is_scrapped = 1), 0) AS scraps, " +
                    "IFNULL(ups.is_liked, 0) AS liked, " +
                    "IFNULL(ups.is_scrapped, 0) AS scrapped " +
                    "FROM problems p " +
                    "JOIN users u ON p.author_id = u.id " +
                    "JOIN user_problem_status ups ON p.id = ups.problem_id " +
                    "WHERE ups.user_id = ?1 AND ups.is_scrapped = 1 " +
                    "ORDER BY ups.updated_at DESC";

            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .getResultList();

            List<Map<String, Object>> scrappedProblems = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> item = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                item.put("id", problemId);
                item.put("title", row[1]);
                item.put("author", row[2]);
                item.put("authorId", row[3]);
                item.put("cardCount", row[4]);
                item.put("likes", row[5]);
                item.put("scraps", row[6]);
                item.put("liked", ((Number) row[7]).intValue() == 1);
                item.put("scrapped", ((Number) row[8]).intValue() == 1);

                // 카테고리 태그와 색상 코드 조회
                String tagsSql = "SELECT c.tag_name, c.color_code FROM categories c " +
                        "JOIN problem_categories pc ON c.id = pc.category_id " +
                        "WHERE pc.problem_id = ?1";
                List<Object[]> tagsAndColors = entityManager.createNativeQuery(tagsSql)
                        .setParameter(1, problemId)
                        .getResultList();

                List<Map<String, String>> categoriesWithColor = new ArrayList<>();
                for (Object[] tagRow : tagsAndColors) {
                    Map<String, String> categoryMap = new HashMap<>();
                    categoryMap.put("tag_name", tagRow[0].toString());
                    categoryMap.put("color_code", tagRow[1] != null ? tagRow[1].toString() : "#CCCCCC");
                    categoriesWithColor.add(categoryMap);
                }
                item.put("categories", categoriesWithColor);

                scrappedProblems.add(item);
            }

            log.info("MyStudyController - getScrappedProblems 성공: userId={}에 대해 {}개의 스크랩 문제 조회됨.", userId, scrappedProblems.size());
            return ResponseEntity.ok(scrappedProblems);

        } catch (Exception e) {
            log.error("MyStudyController - getScrappedProblems: 스크랩 문제 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }


    /**
     * 특정 문제에 대한 학습 상태를 업데이트합니다.
     * PUT /api/mystudy/problems/{problemId}/status
     *
     * @param problemId 문제 ID
     * @param data      업데이트할 학습 상태 ('new', 'ongoing', 'completed') 및 사용자 ID
     * @return 성공/실패 메시지
     */
    @PutMapping("/problems/{problemId}/status")
    public ResponseEntity<Map<String, String>> updateProblemStatus(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - updateProblemStatus 호출됨: problemId={}, data={}", problemId, data);
        Map<String, String> response = new HashMap<>();
        String userId = (String) data.get("userId");
        String newStatus = (String) data.get("status");

        if (userId == null || newStatus == null || problemId == null) {
            log.warn("MyStudyController - updateProblemStatus: 필수 입력값 누락. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, status, problemId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - updateProblemStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateProblemStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // user_problem_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getSingleResult()).longValue();

            if (count > 0) {
                // 기존 레코드가 있으면 업데이트
                String updateSql = "UPDATE user_problem_status SET problem_status = ?1, updated_at = NOW() WHERE user_id = ?2 AND problem_id = ?3";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, newStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
                log.info("MyStudyController - updateProblemStatus: 문제 학습 상태 업데이트 성공. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "문제 학습 상태가 업데이트되었습니다.");
            } else {
                // 기존 레코드가 없으면 삽입 (problem_status와 updated_at만 설정)
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, problem_status, created_at, updated_at) VALUES (?1, ?2, ?3, NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, newStatus)
                        .executeUpdate();
                log.info("MyStudyController - updateProblemStatus: 새 문제 학습 상태 삽입 성공. problemId={}, userId={}, newStatus={}", problemId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "새 문제 학습 상태가 생성되었습니다.");
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MyStudyController - updateProblemStatus: 문제 학습 상태 업데이트/삽입 중 오류 발생 (problemId: {}, internalUserId: {}, newStatus: {}): {}", problemId, internalUserId, newStatus, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 특정 문제의 특정 카드에 대한 학습 상태를 업데이트합니다.
     * PUT /api/mystudy/cards/{cardId}/status
     *
     * @param cardId 카드 ID
     * @param data   업데이트할 카드 상태 ('perfect', 'vague', 'forgotten') 및 사용자 ID
     * @return 성공/실패 메시지
     */
    @PutMapping("/cards/{cardId}/status")
    public ResponseEntity<Map<String, String>> updateCardStatus(
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - updateCardStatus 호출됨: cardId={}, data={}", cardId, data);
        Map<String, String> response = new HashMap<>();
        String userId = (String) data.get("userId");
        String newStatus = (String) data.get("status");

        if (userId == null || newStatus == null || cardId == null) {
            log.warn("MyStudyController - updateCardStatus: 필수 입력값 누락. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, status, cardId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - updateCardStatus: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateCardStatus: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // 카드의 problem_id를 조회
            String getProblemIdSql = "SELECT problem_id FROM cards WHERE id = ?1";
            Long problemId = ((Number) entityManager.createNativeQuery(getProblemIdSql)
                    .setParameter(1, cardId)
                    .getSingleResult()).longValue();

            // user_card_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_card_status WHERE user_id = ?1 AND card_id = ?2 AND problem_id = ?3";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, cardId)
                    .setParameter(3, problemId)
                    .getSingleResult()).longValue();

            if (count > 0) {
                // 기존 레코드가 있으면 업데이트
                String updateSql = "UPDATE user_card_status SET card_status = ?1, updated_at = NOW() WHERE user_id = ?2 AND card_id = ?3 AND problem_id = ?4";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, newStatus)
                        .setParameter(2, internalUserId)
                        .setParameter(3, cardId)
                        .setParameter(4, problemId)
                        .executeUpdate();
                log.info("MyStudyController - updateCardStatus: 카드 학습 상태 업데이트 성공. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "카드 학습 상태가 업데이트되었습니다.");
            } else {
                // 기존 레코드가 없으면 삽입
                String insertSql = "INSERT INTO user_card_status (user_id, problem_id, card_id, card_status, created_at, updated_at) VALUES (?1, ?2, ?3, ?4, NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .setParameter(3, cardId)
                        .setParameter(4, newStatus)
                        .executeUpdate();
                log.info("MyStudyController - updateCardStatus: 새 카드 학습 상태 삽입 성공. cardId={}, userId={}, newStatus={}", cardId, userId, newStatus);
                response.put("status", "OK");
                response.put("message", "새 카드 학습 상태가 생성되었습니다.");
            }
            return ResponseEntity.ok(response);

        } catch (NoResultException e) {
            log.warn("MyStudyController - updateCardStatus: 카드 ID '{}'에 해당하는 카드를 찾을 수 없음.", cardId);
            response.put("status", "ERROR");
            response.put("message", "카드를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - updateCardStatus: 카드 학습 상태 업데이트/삽입 중 오류 발생 (cardId: {}, internalUserId: {}, newStatus: {}): {}", cardId, internalUserId, newStatus, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 특정 문제에 대한 사용자의 학습 상태와 해당 문제 자체를 시스템에서 삭제합니다.
     * 이는 해당 문제와 관련된 사용자의 카드 학습 상태, 모든 사용자의 학습 상태,
     * 해당 문제에 속한 모든 카드, 그리고 문제 자체를 영구적으로 삭제합니다.
     * !!! 경고: 이 작업은 전체 시스템에 영향을 미치며 되돌릴 수 없습니다. !!!
     * DELETE /api/mystudy/problems/{problemId}/status/{userId}
     *
     * @param problemId 삭제할 문제 ID
     * @param userId    현재 로그인한 사용자 ID (users 테이블의 userid 필드)
     * @return 성공/실패 메시지
     */
    @DeleteMapping("/problems/{problemId}/status/{userId}")
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

    /**
     * 특정 문제의 좋아요 상태를 토글합니다. (좋아요 추가 또는 취소)
     * PUT /api/mystudy/problems/{problemId}/toggle-
     *
     *
     * @param problemId 토글할 문제 ID
     * @param data      사용자 ID
     * @return 좋아요 상태 업데이트 결과
     */
    @PutMapping("/problems/{problemId}/toggle-like")
    public ResponseEntity<Map<String, Object>> toggleProblemLike(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - toggleProblemLike 호출됨: problemId={}, data={}", problemId, data);
        Map<String, Object> response = new HashMap<>();
        String userId = (String) data.get("userId");

        if (userId == null || problemId == null) {
            log.warn("MyStudyController - toggleProblemLike: 필수 입력값 누락. problemId={}, userId={}", problemId, userId);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, problemId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - toggleProblemLike: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemLike: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // user_problem_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getSingleResult()).longValue();

            boolean isCurrentlyLiked = false;
            if (count > 0) {
                // 현재 좋아요 상태 조회
                String currentLikedStatusSql = "SELECT is_liked FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
                Object result = entityManager.createNativeQuery(currentLikedStatusSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .getSingleResult();
                isCurrentlyLiked = ((Number) result).intValue() == 1;

                // 좋아요 상태 토글
                String updateSql = "UPDATE user_problem_status SET is_liked = ?, updated_at = NOW() WHERE user_id = ?2 AND problem_id = ?3";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, isCurrentlyLiked ? 0 : 1) // 0: 취소, 1: 좋아요
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            } else {
                // 레코드가 없으면 새로 삽입하고 좋아요 상태를 1로 설정
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, is_liked, problem_status, created_at, updated_at) VALUES (?1, ?2, 1, 'new', NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .executeUpdate();
                isCurrentlyLiked = false; // 새로 추가된 것이므로 이전 상태는 'false'로 간주
            }

            // 업데이트된 좋아요 총 개수 조회
            String totalLikesSql = "SELECT COUNT(*) FROM user_problem_status WHERE problem_id = ?1 AND is_liked = 1";
            Long totalLikes = ((Number) entityManager.createNativeQuery(totalLikesSql)
                    .setParameter(1, problemId)
                    .getSingleResult()).longValue();

            response.put("status", "OK");
            response.put("isLiked", !isCurrentlyLiked); // 토글된 최종 상태 반환
            response.put("totalLikes", totalLikes);
            response.put("message", isCurrentlyLiked ? "좋아요가 취소되었습니다." : "문제를 좋아요했습니다.");
            log.info("MyStudyController - toggleProblemLike 성공: problemId={}, userId={}, isLiked={}, totalLikes={}", problemId, userId, !isCurrentlyLiked, totalLikes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemLike: 좋아요 상태 업데이트 중 오류 발생 (problemId: {}, internalUserId: {}): {}", problemId, internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /**
     * 특정 문제의 스크랩 상태를 토글합니다. (스크랩 추가 또는 취소)
     * PUT /api/mystudy/problems/{problemId}/toggle-scrap
     *
     * @param problemId 토글할 문제 ID
     * @param data      사용자 ID
     * @return 스크랩 상태 업데이트 결과
     */
    @PutMapping("/problems/{problemId}/toggle-scrap")
    public ResponseEntity<Map<String, Object>> toggleProblemScrap(
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> data) {
        log.info("MyStudyController - toggleProblemScrap 호출됨: problemId={}, data={}", problemId, data);
        Map<String, Object> response = new HashMap<>();
        String userId = (String) data.get("userId");

        if (userId == null || problemId == null) {
            log.warn("MyStudyController - toggleProblemScrap: 필수 입력값 누락. problemId={}, userId={}", problemId, userId);
            response.put("status", "ERROR");
            response.put("message", "필수 입력값(userId, problemId)이 누락되었습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Long internalUserId;
        try {
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MyStudyController - toggleProblemScrap: 사용자 ID '{}'를 찾을 수 없음.", userId);
            response.put("status", "ERROR");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemScrap: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // user_problem_status 테이블에 해당 레코드가 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
            Long count = ((Number) entityManager.createNativeQuery(checkSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, problemId)
                    .getSingleResult()).longValue();

            boolean isCurrentlyScrapped = false;
            if (count > 0) {
                // 현재 스크랩 상태 조회
                String currentScrappedStatusSql = "SELECT is_scrapped FROM user_problem_status WHERE user_id = ?1 AND problem_id = ?2";
                Object result = entityManager.createNativeQuery(currentScrappedStatusSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .getSingleResult();
                isCurrentlyScrapped = ((Number) result).intValue() == 1;

                // 스크랩 상태 토글
                String updateSql = "UPDATE user_problem_status SET is_scrapped = ?, updated_at = NOW() WHERE user_id = ?2 AND problem_id = ?3";
                entityManager.createNativeQuery(updateSql)
                        .setParameter(1, isCurrentlyScrapped ? 0 : 1) // 0: 취소, 1: 스크랩
                        .setParameter(2, internalUserId)
                        .setParameter(3, problemId)
                        .executeUpdate();
            } else {
                // 레코드가 없으면 새로 삽입하고 스크랩 상태를 1로 설정
                String insertSql = "INSERT INTO user_problem_status (user_id, problem_id, is_scrapped, problem_status, created_at, updated_at) VALUES (?1, ?2, 1, 'new', NOW(), NOW())";
                entityManager.createNativeQuery(insertSql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, problemId)
                        .executeUpdate();
                isCurrentlyScrapped = false; // 새로 추가된 것이므로 이전 상태는 'false'로 간주
            }

            // 업데이트된 스크랩 총 개수 조회
            String totalScrapsSql = "SELECT COUNT(*) FROM user_problem_status WHERE problem_id = ?1 AND is_scrapped = 1";
            Long totalScraps = ((Number) entityManager.createNativeQuery(totalScrapsSql)
                    .setParameter(1, problemId)
                    .getSingleResult()).longValue();

            response.put("status", "OK");
            response.put("isScrapped", !isCurrentlyScrapped); // 토글된 최종 상태 반환
            response.put("totalScraps", totalScraps);
            response.put("message", isCurrentlyScrapped ? "스크랩이 취소되었습니다." : "문제를 스크랩했습니다.");
            log.info("MyStudyController - toggleProblemScrap 성공: problemId={}, userId={}, isScrapped={}, totalScraps={}", problemId, userId, !isCurrentlyScrapped, totalScraps);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("MyStudyController - toggleProblemScrap: 스크랩 상태 업데이트 중 오류 발생 (problemId: {}, internalUserId: {}): {}", problemId, internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("status", "ERROR");
            response.put("message", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}