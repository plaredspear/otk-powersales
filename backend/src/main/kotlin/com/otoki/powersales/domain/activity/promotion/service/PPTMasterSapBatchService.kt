package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterPayloadFactory
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.sender.PPTMasterSapSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 전문행사조 마스터 SAP 송신 batch service (Spec #765).
 *
 * 매시간 cron 으로 발화되어 당월 활성 마스터를 SAP `/SD03300` 으로 송신한다.
 * - 송신 대상 조회 → page 분할 → payload build → sender 호출 → 결과 metadata 기록
 * - 레거시 `IF_REST_SAP_PPTMToSAP.cls:22-36` SOQL 의 실효 동작 1:1 정합 (당월 기간 조건만 적용 — Q1 노선 A)
 * - AttendanceBatchService 패턴 정합 (3-tier: batch / service / sender)
 * - 결과 0건이면 sender 호출 안 함 + INFO 로그 + metadata totalRows=0 기록
 * - sender 실패는 throw 하지 않음 — `failedPages` 누적 후 다음 page 계속. cron 다음 fire (1시간 후) 가 자연 재시도.
 */
@Service
class PPTMasterSapBatchService(
    private val pptMasterRepository: PPTMasterRepository,
    private val payloadFactory: PPTMasterPayloadFactory,
    private val sender: PPTMasterSapSender,
    @Value("\${app.sap.outbound.ppt-master.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(PPTMasterSapBatchService::class.java)

    fun runHourly(context: ScheduledJobRunContext? = null) {
        runHourly(LocalDate.now(), context)
    }

    internal fun runHourly(today: LocalDate, context: ScheduledJobRunContext? = null) {
        val monthFirstDay = today.withDayOfMonth(1)
        val monthLastDay = monthFirstDay.plusMonths(1).minusDays(1)

        val targets = pptMasterRepository.findSapOutboundTargets(monthFirstDay, monthLastDay)
        val totalRows = targets.size

        if (totalRows == 0) {
            log.info("PPT_MASTER_SAP_BATCH today={} totalRows=0 (skip)", today)
            context?.metadata(mapOf("totalRows" to 0, "totalPages" to 0, "sentPages" to 0, "failedPages" to 0))
            return
        }

        var sentPages = 0
        var failedPages = 0
        val pages = targets.chunked(pageSize)
        pages.forEach { page ->
            val payload = payloadFactory.build(page, today)
            val ok = sender.sendPage(payload)
            if (ok) sentPages++ else failedPages++
        }

        val totalPages = pages.size
        log.info(
            "PPT_MASTER_SAP_BATCH today={} totalRows={} pages={} sent={} failed={}",
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
