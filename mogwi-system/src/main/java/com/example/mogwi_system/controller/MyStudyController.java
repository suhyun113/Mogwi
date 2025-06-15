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
                    "LEFT JOIN user_card_status ucs ON p.id = ucs.problem_id AND ucs.user_id = ?2 " +
                    "WHERE p.id IN (" +
                    "SELECT p_all.id FROM problems p_all " +
                    "LEFT JOIN user_problem_status ups_all ON p_all.id = ups_all.problem_id AND ups_all.user_id = ?3 " +
                    "WHERE ups_all.user_id IS NULL OR ups_all.problem_status IN ('new', 'ongoing', 'completed')" +
                    ") " +
                    "GROUP BY p.id, p.title, p.description, p.card_count, u.username, ups.is_liked, ups.is_scrapped, ups.problem_status " +
                    "ORDER BY IFNULL(ups.updated_at, p.created_at) DESC";

            List<Object[]> problemResults = entityManager.createNativeQuery(problemSql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, internalUserId)
                    .setParameter(3, internalUserId)
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

                // 카테고리 태그와 색상 코드 조회
                // SQL 쿼리에 color_code 컬럼 추가
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
                    categoryMap.put("color_code", tagRow[1] != null ? tagRow[1].toString() : "#CCCCCC"); // null 처리 및 기본값 설정
                    categoriesWithColor.add(categoryMap);
                }
                problem.put("categories", categoriesWithColor); // categories를 List<Map<String, String>>으로 변경

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
}