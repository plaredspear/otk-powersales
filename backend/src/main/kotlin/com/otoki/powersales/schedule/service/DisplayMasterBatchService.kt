package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.schedule.sap.DisplayMasterSapPayloadRow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DisplayMasterBatchService(
    private val repository: DisplayWorkScheduleRepository,
    private val payloadFactory: DisplayMasterPayloadFactory,
    private val sender: DisplayMasterSapSender,
    @Value("\${app.sap.outbound.display.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(DisplayMasterBatchService::class.java)

    fun runDaily(context: ScheduledJobRunContext? = null) {
        runDaily(LocalDate.now(), context)
    }

    internal fun runDaily(today: LocalDate, context: ScheduledJobRunContext? = null) {
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
}
