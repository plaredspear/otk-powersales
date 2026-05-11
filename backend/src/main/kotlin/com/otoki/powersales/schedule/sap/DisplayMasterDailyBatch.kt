package com.otoki.powersales.schedule.sap

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 진열 마스터(DISPLAY) daily SAP outbound batch (Spec #588 P2-B §1.2).
 *
 * - cron: `app.sap.outbound.display.cron` (기본 `0 0 1 * * *`)
 * - 페이지 단위 (`app.sap.outbound.display.page-size`, 기본 100) 로 SAP 송신
 * - ShedLock 으로 동시 실행 방지 (`display-master-sap-batch`)
 * - 페이지 실패 시 다음 페이지 진행 (재시도 없음 — 익일 batch 자연 멱등 의존)
 */
@Component
class DisplayMasterDailyBatch(
    private val repository: DisplayWorkScheduleRepository,
    private val payloadFactory: DisplayMasterPayloadFactory,
    private val sender: DisplayMasterSapSender,
    private val scheduledJobRunner: ScheduledJobRunner,
    @Value("\${app.sap.outbound.display.page-size:100}") private val pageSize: Int
) {

    private val log = LoggerFactory.getLogger(DisplayMasterDailyBatch::class.java)

    @Scheduled(cron = "\${app.sap.outbound.display.cron:0 0 1 * * *}")
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
        var pageIndex = 0
        var totalRows = 0
        var sentPages = 0
        var failedPages = 0

        while (true) {
            val entities = repository.findValidForDisplayMasterSapPaged(
                date = today,
                limit = pageSize,
                offset = pageIndex * pageSize
            )
            if (entities.isEmpty()) break

            val rows = entities.map { it.toSapPayloadRow() }
            totalRows += rows.size
            val payload = payloadFactory.build(rows, today)
            val ok = sender.sendPage(payload)
            if (ok) sentPages++ else failedPages++

            if (entities.size < pageSize) break
            pageIndex++
        }

        val totalPages = sentPages + failedPages
        log.info(
            "DISPLAY_SAP_BATCH today={} totalRows={} pages={} sent={} failed={}",
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

    private fun DisplayWorkSchedule.toSapPayloadRow(): DisplayMasterSapPayloadRow =
        DisplayMasterSapPayloadRow(
            displayWorkScheduleId = id,
            employeeCode = employee?.employeeCode,
            accountExternalKey = account?.externalKey,
            typeOfWork1 = typeOfWork1,
            typeOfWork3 = typeOfWork3,
            typeOfWork5 = typeOfWork5
        )

    companion object {
        const val JOB_NAME = "display-master-sap-batch"
    }
}
