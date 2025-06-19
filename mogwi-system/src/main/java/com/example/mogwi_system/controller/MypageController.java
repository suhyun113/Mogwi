package com.example.mogwi_system.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/mypage") // MypageController의 기본 경로 설정
public class MypageController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Helper method: 외부 사용자 ID(userid)로 내부 사용자 ID(id)를 조회합니다.
     *
     * @param userId 외부 사용자 ID
     * @return 내부 사용자 ID
     * @throws NoResultException 해당 사용자 ID를 찾을 수 없는 경우
     * @throws RuntimeException 데이터베이스 조회 중 오류 발생 시
     */
    private Long getInternalUserId(String userId) throws NoResultException {
        log.info("MypageController: 외부 사용자 ID '{}'에 대한 내부 ID 조회 시도", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("MypageController: 외부 사용자 ID '{}'에 해당하는 내부 사용자를 찾을 수 없음", userId);
            throw e;
        } catch (Exception e) {
            log.error("MypageController: 외부 사용자 ID '{}'에 대한 내부 사용자 ID 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("내부 사용자 ID를 검색하는 데 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자가 좋아요/스크랩한 문제 목록을 조회합니다.
     * GET /api/mypage/problems/liked-scraped/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 좋아요 및 스크랩된 문제 목록
     */
    @GetMapping("/problems/liked-scraped/{userId}")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getLikedScrapedProblems(
            @PathVariable String userId) {
        log.info("MypageController - getLikedScrapedProblems 호출됨: userId={}", userId);
        Long internalUserId;

        Map<String, List<Map<String, Object>>> errorResponse = new HashMap<>();
        errorResponse.put("likedProblems", new ArrayList<>());
        errorResponse.put("scrapedProblems", new ArrayList<>());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("MypageController - getLikedScrapedProblems: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MypageController - getLikedScrapedProblems: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("MypageController - getLikedScrapedProblems: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        List<Map<String, Object>> likedProblems = new ArrayList<>();
        List<Map<String, Object>> scrapedProblems = new ArrayList<>();

        try {
            // ProblemListItem for Mypage's LikedScrapSection expects: id, title, is_public, topic
            String sql = "SELECT " +
                    "p.id, p.title, p.is_public, " +
                    "COALESCE(ups.is_liked, 0) AS is_liked_by_user, " +
                    "COALESCE(ups.is_scrapped, 0) AS is_scrapped_by_user " +
                    "FROM problems p " +
                    "JOIN user_problem_status ups ON p.id = ups.problem_id " +
                    "WHERE ups.user_id = ?1 AND (ups.is_liked = 1 OR ups.is_scrapped = 1) " +
                    "ORDER BY ups.updated_at DESC";

            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .getResultList();

            for (Object[] row : results) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                // is_public 필드는 Integer로 반환될 수 있으므로 명시적으로 Boolean으로 변환
                problem.put("is_public", ((Number) row[2]).intValue() == 1); // <-- 이 부분이 수정되었습니다.

                String topicSql = "SELECT c.tag_name FROM categories c " +
                        "JOIN problem_categories pc ON c.id = pc.category_id " +
                        "WHERE pc.problem_id = ?1 LIMIT 1";
                try {
                    String topic = (String) entityManager.createNativeQuery(topicSql)
                            .setParameter(1, problemId)
                            .getSingleResult();
                    problem.put("topic", topic);
                } catch (NoResultException e) {
                    problem.put("topic", null);
                }

                // isLiked와 isScrapped는 이미 Number로 조회되므로 intValue()를 사용한 변환 유지
                boolean isLiked = ((Number) row[3]).intValue() == 1;
                boolean isScrapped = ((Number) row[4]).intValue() == 1;

                if (isLiked) {
                    likedProblems.add(problem);
                }
                if (isScrapped) {
                    scrapedProblems.add(problem);
                }
            }

            Map<String, List<Map<String, Object>>> responseBody = new HashMap<>();
            responseBody.put("likedProblems", likedProblems);
            responseBody.put("scrapedProblems", scrapedProblems);

            log.info("MypageController - getLikedScrapedProblems 성공: userId={}, 좋아요 {}개, 스크랩 {}개 조회됨.", userId, likedProblems.size(), scrapedProblems.size());
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("MypageController - getLikedScrapedProblems: 좋아요/스크랩 문제 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /**
     * 특정 사용자가 생성한 문제 목록을 조회합니다.
     * GET /api/mypage/problems/my-created/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 내가 만든 문제 목록 (공개/비공개 포함)
     */
    @GetMapping("/problems/my-created/{userId}")
    public ResponseEntity<Map<String, List<Map<String, Object>>> > getMyCreatedProblems(
            @PathVariable String userId) {
        log.info("MypageController - getMyCreatedProblems 호출됨: userId={}", userId);
        Long internalUserId;

        Map<String, List<Map<String, Object>>> errorResponse = new HashMap<>();
        errorResponse.put("myProblems", new ArrayList<>());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("MypageController - getMyCreatedProblems: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("MypageController - getMyCreatedProblems: 사용자 ID '{}'를 찾을 수 없음. 유효하지 않은 userId 요청.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("MypageController - getMyCreatedProblems: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        List<Map<String, Object>> myCreatedProblems = new ArrayList<>();

        try {
            // ProblemListItem for Mypage's MyProblemSection expects: id, title, is_public, topic
            String sql = "SELECT " +
                    "p.id, p.title, p.is_public " +
                    "FROM problems p " +
                    "WHERE p.author_id = ?1 " + // Filter by current user as author
                    "ORDER BY p.created_at DESC";

            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .getResultList();

            for (Object[] row : results) {
                Map<String, Object> problem = new HashMap<>();
                Long problemId = ((Number) row[0]).longValue();
                problem.put("id", problemId);
                problem.put("title", row[1]);
                // is_public 필드는 Integer로 반환될 수 있으므로 명시적으로 Boolean으로 변환
                problem.put("is_public", ((Number) row[2]).intValue() == 1); // <-- 이 부분이 수정되었습니다.

                String topicSql = "SELECT c.tag_name FROM categories c " +
                        "JOIN problem_categories pc ON c.id = pc.category_id " +
                        "WHERE pc.problem_id = ?1 LIMIT 1";
                try {
                    String topic = (String) entityManager.createNativeQuery(topicSql)
                            .setParameter(1, problemId)
                            .getSingleResult();
                    problem.put("topic", topic);
                } catch (NoResultException e) {
                    problem.put("topic", null);
                }

                myCreatedProblems.add(problem);
            }

            Map<String, List<Map<String, Object>>> responseBody = new HashMap<>();
            responseBody.put("myProblems", myCreatedProblems);

            log.info("MypageController - getMyCreatedProblems 성공: userId={}에 대해 {}개의 내가 만든 문제 조회됨.", userId, myCreatedProblems.size());
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("MypageController - getMyCreatedProblems: 내가 만든 문제 목록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
