package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.internal.LastMonthRevenueLookup
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * DisplayWorkSchedule.lastMonthRevenue daily batch service (spec #690 — legacy `UpdateLastMonthRevenueBatch` 동등 복원).
 *
 * 매일 fire (cron 사용자 결정 daily — 매출 적재 시점 의존성 회피) 으로 ValidData='유효' 마스터 (Confirmed
 * 조건 없음 — SAP outbound batch 와 다름, legacy SOQL 100% 동등) 의 last_month_revenue 컬럼을 직전월
 * 매출로 갱신.
 *
 * - 페이지 조회: [com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepositoryCustom.findValidForLastMonthRevenuePaged] (#669 의
 *   `validDataEqualsValid` 헬퍼 재활용).
 * - 매출 lookup: [LastMonthRevenueLookup.forAccounts] (#784 helper 재활용).
 * - first-run 가드: `IS NULL OR compareTo != 0` 만 update (null vs 0 폭증 회피).
 * - update 패턴: [com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepositoryCustom.updateLastMonthRevenueById] native UPDATE
 *   (last_month_revenue 컬럼만 — JPA save 회피로 BaseEntity 의 updated_at 자동 갱신 영향 0,
 *   legacy `TriggerHandler.bypass` 의미 정합).
 */
@Service
class DisplayMasterLastMonthRevenueBatchService(
    private val repository: DisplayWorkScheduleRepository,
    private val lastMonthRevenueLookup: LastMonthRevenueLookup,
    @Value("\${app.batch.display.last-month-revenue.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(DisplayMasterLastMonthRevenueBatchService::class.java)

    fun runDaily(context: ScheduledJobRunContext? = null) {
        runDaily(LocalDate.now(), context)
    }

    internal fun runDaily(today: LocalDate, context: ScheduledJobRunContext? = null) {
        var pageIndex = 0
        var totalRows = 0
        var updatedRows = 0
        var skippedRows = 0
        var failedRows = 0

        while (true) {
            val entities = repository.findValidForLastMonthRevenuePaged(
                date = today,
                limit = pageSize,
                offset = pageIndex * pageSize
            )
            if (entities.isEmpty()) break
            totalRows += entities.size

            val accounts = entities.mapNotNull { it.account }.distinctBy { it.id }
            val revenueByAccountId = lastMonthRevenueLookup.forAccounts(accounts, today)

            for (schedule in entities) {
                val newRevenue = schedule.account?.let { revenueByAccountId[it.id] } ?: BigDecimal.ZERO
                val current = schedule.lastMonthRevenue
                val shouldUpdate = current == null || current.compareTo(newRevenue) != 0
                if (!shouldUpdate) {
                    skippedRows++
                    continue
                }
                try {
                    repository.updateLastMonthRevenueById(schedule.id, newRevenue)
                    updatedRows++
                } catch (ex: Exception) {
                    failedRows++
                    log.warn(
                        "DISPLAY_LAST_MONTH_REVENUE_BATCH update failed id={} accountId={} reason={}",
                        schedule.id, schedule.account?.id, ex.message
                    )
                }
            }

            if (entities.size < pageSize) break
            pageIndex++
        }

        log.info(
            "DISPLAY_LAST_MONTH_REVENUE_BATCH today={} totalRows={} updated={} skipped={} failed={}",
            today, totalRows, updatedRows, skippedRows, failedRows
        )
        context?.metadata(
            mapOf(
                "totalRows" to totalRows,
                "updatedRows" to updatedRows,
                "skippedRows" to skippedRows,
                "failedRows" to failedRows
            )
        )
    }
}
