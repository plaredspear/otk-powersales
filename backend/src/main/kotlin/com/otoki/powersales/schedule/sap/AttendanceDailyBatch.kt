package com.otoki.powersales.schedule.sap

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.sap.outbound.sender.AttendanceSapSender
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 일반 출근(REGULAR) daily SAP outbound batch (Spec #588 P1-B §1.2).
 *
 * - cron: `app.sap.outbound.attendance.cron` (기본 `0 0 1 * * *`)
 * - 페이지 단위 (`app.sap.outbound.attendance.page-size`, 기본 100) 로 SAP 송신
 * - ShedLock 으로 동시 실행 방지 (`attendance-sap-batch`)
 * - 페이지 실패 시 다음 페이지 진행 + 배치 종료 후 실패 페이지 수 로깅
 *   (재시도 없음 — 익일 batch 자연 멱등 의존, spec #588 §8 Q4)
 */
@Component
class AttendanceDailyBatch(
    private val repository: TeamMemberScheduleRepository,
    private val payloadFactory: AttendancePayloadFactory,
    private val sender: AttendanceSapSender,
    private val scheduledJobRunner: ScheduledJobRunner,
    @Value("\${app.sap.outbound.attendance.page-size:100}") private val pageSize: Int
) {

    private val log = LoggerFactory.getLogger(AttendanceDailyBatch::class.java)

    @Scheduled(cron = "\${app.sap.outbound.attendance.cron:0 0 1 * * *}")
    @SchedulerLock(
        name = JOB_NAME,
        lockAtMostFor = "PT10M",
        lockAtLeastFor = "PT30S"
    )
    fun runDaily() {
        scheduledJobRunner.run(JOB_NAME) { context ->
            execute(LocalDate.now(), context)
        }
    }

    internal fun execute(today: LocalDate, context: ScheduledJobRunContext? = null) {
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

    companion object {
        const val JOB_NAME = "attendance-sap-batch"
    }
}
