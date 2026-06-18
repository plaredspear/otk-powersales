package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.AttendancePayloadFactory
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.sender.AttendanceSapSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 여사원일정 스케줄 SAP전송 배치 (SD03130) 의 일배치 실행 서비스.
 *
 * 레거시 `Batch_TeamMemberSchedule.cls` (스케줄러 = 레거시 운영 잡 "여사원일정 스케줄") 동등.
 *
 * **실행 시각 정합 (검토: 2026-06-18)**
 * - 레거시 운영 cron = `0 10 0 * * ? *` (매일 00:10 KST, org TZ=Asia/Seoul).
 *   (참고: 레거시 테스트 코드의 `0 0 23` 은 운영과 무관한 값이라 신뢰하면 안 됨.)
 * - 신규 cron = 매일 01:00 KST (JVM TZ=Asia/Seoul 고정).
 * - 둘 다 **자정 직후**라 같은 달력일을 `today` 로 인식 → today/yesterday 날짜 윈도우 라벨이 어긋나지 않음.
 *
 * **날짜 조회 조건 정합 (검토: 2026-06-18)**
 * - 당일 = 실행일(`WorkingDate = today`), 어제 = 실행일 −1일(`WorkingDate = yesterday`) — 레거시와 동일.
 * - 레거시 어제 분기는 SOQL `Yesterday` 리터럴을 쓴다. 신규의 [LocalDate.minusDays] 는 월·연 경계를
 *   자동으로 넘기므로(예: 11/1 → 10/31) 동일 결과이며, 레거시가 과거 문자열 조립으로 겪었던
 *   월말 경계 버그(`2023-11-31`)를 구조적으로 회피한다.
 * - 분기별 부가 조건(당일=commute_log 무관 전송 / 어제=commute_log 연결분 + WorkingCategory4)은
 *   [TeamMemberScheduleRepository.findRegularAttendancesForSapPaged] 쿼리 주석 참조.
 */
@Service
class AttendanceBatchService(
    private val repository: TeamMemberScheduleRepository,
    private val payloadFactory: AttendancePayloadFactory,
    private val sender: AttendanceSapSender,
    @Value("\${app.sap.outbound.attendance.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(AttendanceBatchService::class.java)

    fun runDaily(context: ScheduledJobRunContext? = null) {
        // today = 실행일(KST). 레거시 `Date.today()` 와 동일 기준 — 자정 직후 실행이라 같은 달력일.
        runDaily(LocalDate.now(), context)
    }

    internal fun runDaily(today: LocalDate, context: ScheduledJobRunContext? = null) {
        // 레거시 SOQL `Yesterday` 리터럴과 동등. LocalDate 연산이라 월·연 경계 자동 처리(11/1→10/31).
        val yesterday = today.minusDays(1)
        var pageIndex = 0
        var totalRows = 0
        var sentPages = 0
        var failedPages = 0

        while (true) {
            val rows = repository.findRegularAttendancesForSapPaged(
                today = today,
                yesterday = yesterday,
                limit = pageSize,
                offset = pageIndex * pageSize
            )
            if (rows.isEmpty()) break

            totalRows += rows.size
            val payload = payloadFactory.build(rows, today)
            val ok = sender.sendPage(payload)
            if (ok) sentPages++ else failedPages++

            if (rows.size < pageSize) break
            pageIndex++
        }

        val totalPages = sentPages + failedPages
        log.info(
            "ATT_SAP_BATCH today={} totalRows={} pages={} sent={} failed={}",
            today, totalRows, totalPages, sentPages, failedPages
        )
        context?.metadata(
            mapOf(
                "totalRows" to totalRows,
                "totalPages" to totalPages,
                "sentPages" to sentPages,
                "failedPages" to failedPages
            )
        )
    }
}
