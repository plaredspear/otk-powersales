package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.sender.AttendanceSapSender
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.sap.AttendancePayloadFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AttendanceBatchService(
    private val repository: TeamMemberScheduleRepository,
    private val payloadFactory: AttendancePayloadFactory,
    private val sender: AttendanceSapSender,
    @Value("\${app.sap.outbound.attendance.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(AttendanceBatchService::class.java)

    fun runDaily(context: ScheduledJobRunContext? = null) {
        runDaily(LocalDate.now(), context)
    }

    internal fun runDaily(today: LocalDate, context: ScheduledJobRunContext? = null) {
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
