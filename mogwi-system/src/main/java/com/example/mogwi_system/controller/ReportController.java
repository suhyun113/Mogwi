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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Transactional
@RequestMapping("/api/report") // 학습 리포트 API의 기본 경로 설정
@CrossOrigin(origins = "http://localhost:5173") // 필요에 따라 프론트엔드 포트 변경
public class ReportController {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper method: 외부 사용자 ID로 내부 사용자 ID를 조회
    private Long getInternalUserId(String userId) throws NoResultException {
        log.info("ReportController: 외부 사용자 ID '{}'에 대한 내부 ID 조회 시도", userId);
        try {
            Object result = entityManager.createNativeQuery("SELECT id FROM users WHERE userid = ?1")
                    .setParameter(1, userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        } catch (NoResultException e) {
            log.warn("ReportController: 외부 사용자 ID '{}'에 해당하는 내부 사용자를 찾을 수 없음", userId);
            throw e;
        } catch (Exception e) {
            log.error("ReportController: 외부 사용자 ID '{}'에 대한 내부 사용자 ID 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("내부 사용자 ID를 검색하는 데 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자의 전체 학습 요약 (완벽, 희미, 사라진 카드 수)을 조회합니다.
     * GET /api/report/summary/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 학습 요약 데이터 (perfect, vague, forgotten, total 카드 수)
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<Map<String, Object>> getOverallStudySummary(
            @PathVariable String userId) {
        log.info("ReportController - getOverallStudySummary 호출됨: userId={}", userId);
        Map<String, Object> response = new HashMap<>();
        Long internalUserId;

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("ReportController - getOverallStudySummary: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "사용자 ID가 필요합니다."));
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("ReportController - getOverallStudySummary: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("ReportController - getOverallStudySummary: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }

        try {
            String sql = "SELECT " +
                    "SUM(CASE WHEN card_status = 'perfect' THEN 1 ELSE 0 END) AS perfect_count, " +
                    "SUM(CASE WHEN card_status = 'vague' THEN 1 ELSE 0 END) AS vague_count, " +
                    "SUM(CASE WHEN card_status = 'forgotten' THEN 1 ELSE 0 END) AS forgotten_count, " +
                    "COUNT(*) AS total_cards " +
                    "FROM user_card_status " +
                    "WHERE user_id = ?1";

            Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .getSingleResult();

            response.put("perfect", result[0] != null ? ((Number) result[0]).intValue() : 0);
            response.put("vague", result[1] != null ? ((Number) result[1]).intValue() : 0);
            response.put("forgotten", result[2] != null ? ((Number) result[2]).intValue() : 0);
            response.put("total", result[3] != null ? ((Number) result[3]).intValue() : 0);

            log.info("ReportController - getOverallStudySummary 성공: userId={}, perfect={}, vague={}, forgotten={}, total={}",
                    userId, response.get("perfect"), response.get("vague"), response.get("forgotten"), response.get("total"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("ReportController - getOverallStudySummary: 학습 요약 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 특정 사용자의 일별 학습 기록을 조회합니다.
     * GET /api/report/daily-records/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 일별 학습 기록 목록 (date, perfect, vague, forgotten)
     */
    @GetMapping("/daily-records/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getDailyStudyRecords(
            @PathVariable String userId) {
        log.info("ReportController - getDailyStudyRecords 호출됨: userId={}", userId);
        List<Map<String, Object>> dailyRecords = new ArrayList<>();
        Long internalUserId;

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("ReportController - getDailyStudyRecords: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("ReportController - getDailyStudyRecords: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("ReportController - getDailyStudyRecords: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            // 최근 1년간의 데이터만 조회하도록 제한 (성능 고려)
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            String sql = "SELECT " +
                    "DATE_FORMAT(updated_at, '%Y-%m-%d') AS record_date, " +
                    "SUM(CASE WHEN card_status = 'perfect' THEN 1 ELSE 0 END) AS perfect_count, " +
                    "SUM(CASE WHEN card_status = 'vague' THEN 1 ELSE 0 END) AS vague_count, " +
                    "SUM(CASE WHEN card_status = 'forgotten' THEN 1 ELSE 0 END) AS forgotten_count " +
                    "FROM user_card_status " +
                    "WHERE user_id = ?1 AND updated_at >= ?2 " +
                    "GROUP BY record_date " +
                    "ORDER BY record_date ASC";

            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, internalUserId)
                    .setParameter(2, oneYearAgo.format(formatter))
                    .getResultList();

            for (Object[] row : results) {
                Map<String, Object> record = new HashMap<>();
                record.put("date", row[0].toString());
                record.put("perfect", ((Number) row[1]).intValue());
                record.put("vague", ((Number) row[2]).intValue());
                record.put("forgotten", ((Number) row[3]).intValue());
                dailyRecords.add(record);
            }

            log.info("ReportController - getDailyStudyRecords 성공: userId={}에 대해 {}개의 일별 기록 조회됨.", userId, dailyRecords.size());
            return ResponseEntity.ok(dailyRecords);

        } catch (Exception e) {
            log.error("ReportController - getDailyStudyRecords: 일별 학습 기록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * 특정 사용자의 주간 학습 기록을 조회합니다. (최근 5주)
     * GET /api/report/weekly-records/{userId}
     *
     * @param userId 현재 로그인한 사용자의 ID (users 테이블의 userid 필드)
     * @return 주간 학습 기록 목록 (weekStart, perfect, vague, forgotten, total)
     */
    @GetMapping("/weekly-records/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyStudyRecords(
            @PathVariable String userId) {
        log.info("ReportController - getWeeklyStudyRecords 호출됨: userId={}", userId);
        List<Map<String, Object>> weeklyRecords = new ArrayList<>();
        Long internalUserId;

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("ReportController - getWeeklyStudyRecords: userId가 null이거나 비어있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
            }
            internalUserId = getInternalUserId(userId);
        } catch (NoResultException e) {
            log.warn("ReportController - getWeeklyStudyRecords: 사용자 ID '{}'를 찾을 수 없음.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        } catch (Exception e) {
            log.error("ReportController - getWeeklyStudyRecords: 사용자 ID 조회 중 예상치 못한 오류 (userId: {}): {}", userId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }

        try {
            // 현재 날짜를 기준으로 지난 5주를 계산
            LocalDate today = LocalDate.now();
            List<LocalDate> weekStarts = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 지난 5주차의 월요일부터 계산 (일요일을 주의 시작으로 하는 경우도 있지만, 일반적으로 월요일이 더 흔함)
            // 현재 주가 아닌, 지난 5개 주를 포함하도록 조정
            for (int i = 0; i < 5; i++) { // Fetch data for the last 5 weeks
                // Go back 'i' weeks from today
                LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                weekStarts.add(weekStart);
            }
            // 가장 오래된 주부터 정렬
            weekStarts.sort(null);

            for (LocalDate weekStart : weekStarts) {
                LocalDate weekEnd = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

                String sql = "SELECT " +
                        "SUM(CASE WHEN card_status = 'perfect' THEN 1 ELSE 0 END) AS perfect_count, " +
                        "SUM(CASE WHEN card_status = 'vague' THEN 1 ELSE 0 END) AS vague_count, " +
                        "SUM(CASE WHEN card_status = 'forgotten' THEN 1 ELSE 0 END) AS forgotten_count " +
                        "FROM user_card_status " +
                        "WHERE user_id = ?1 AND updated_at BETWEEN ?2 AND ?3";

                Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                        .setParameter(1, internalUserId)
                        .setParameter(2, weekStart.format(formatter) + " 00:00:00") // 시작일 00:00:00
                        .setParameter(3, weekEnd.format(formatter) + " 23:59:59") // 종료일 23:59:59
                        .getSingleResult();

                Map<String, Object> record = new HashMap<>();
                record.put("weekStart", weekStart.format(formatter));
                int perfect = result[0] != null ? ((Number) result[0]).intValue() : 0;
                int vague = result[1] != null ? ((Number) result[1]).intValue() : 0;
                int forgotten = result[2] != null ? ((Number) result[2]).intValue() : 0;
                record.put("perfect", perfect);
                record.put("vague", vague);
                record.put("forgotten", forgotten);
                record.put("total", perfect + vague + forgotten); // 주간 총 학습 카드 수 추가
                weeklyRecords.add(record);
            }

            log.info("ReportController - getWeeklyStudyRecords 성공: userId={}에 대해 {}개의 주간 기록 조회됨.", userId, weeklyRecords.size());
            return ResponseEntity.ok(weeklyRecords);

        } catch (Exception e) {
            log.error("ReportController - getWeeklyStudyRecords: 주간 학습 기록 조회 중 오류 발생 (internalUserId: {}): {}", internalUserId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
}