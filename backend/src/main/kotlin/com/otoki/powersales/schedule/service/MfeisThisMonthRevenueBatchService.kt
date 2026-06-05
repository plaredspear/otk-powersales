package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * MFEIS `this_month_amount` 월간 일괄 갱신 batch service (spec #680 §5.2).
 *
 * legacy `UpdateThisMonthRevenueBatch` (Schedulable + Batchable) 동등 복원 —
 * 매월 fire 로 전월 기준 "상시" MFEIS 의 거래처별 6개월 매출 평균 (양수 필터) 을
 * `this_month_amount` 컬럼에 persist. 차이 있는 row 만 update.
 *
 * cron 사용자 결정 (#680 Q15 옵션 1): `0 0 3 1 * ?` 매월 1일 03:00.
 * ShedLock (#680 Q13 옵션 1): chunk 200 + `PT30M / PT5M`.
 *
 * `MfeisThisMonthRevenueBatch` entry 클래스가 ScheduledJobRunner 위임 + audit log
 * 책임. 본 service 는 비즈니스 로직 + DB I/O 만 담당.
 */
@Service
class MfeisThisMonthRevenueBatchService(
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    @Value("\${app.batch.mfeis.this-month-revenue.chunk-size:200}") private val chunkSize: Int,
) {

    private val log = LoggerFactory.getLogger(MfeisThisMonthRevenueBatchService::class.java)

    fun runMonthly(context: ScheduledJobRunContext? = null) {
        runMonthly(YearMonth.now().minusMonths(1), context)
    }

    internal fun runMonthly(targetYearMonth: YearMonth, context: ScheduledJobRunContext? = null) {
        val yearStr = targetYearMonth.year.toString()
        val monthStr = String.format("%02d", targetYearMonth.monthValue)

        val targets = mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing(
            yearStr, monthStr, "%상시%"
        )

        val totalRows = targets.size
        var updatedRows = 0
        var skippedRows = 0
        var failedRows = 0

        // 6개월 평균 매출 — legacy `UpdateThisMonthRevenueBatch.execute` 의 -5 ~ 0 월 범위 동등.
        // (lastYearMonth 가 전월이므로 targetYearMonth = 전월. 6개월 범위는 [targetYM - 5, targetYM])
        val startYm = targetYearMonth.minusMonths(5)
        val endYm = targetYearMonth
        val salesDates = mutableListOf<String>()
        var current = startYm
        while (!current.isAfter(endYm)) {
            salesDates.add("%d%02d".format(current.year, current.monthValue))
            current = current.plusMonths(1)
        }

        targets.chunked(chunkSize).forEach { chunk ->
            val accounts = chunk.mapNotNull { it.account }.distinctBy { it.id }
            val externalKeyToId: Map<String, Long> = accounts
                .filter { it.externalKey != null }
                .associate { it.externalKey!! to it.id }
            if (externalKeyToId.isEmpty()) {
                skippedRows += chunk.size
                return@forEach
            }

            val histories = monthlySalesHistoryGateway.findBySalesDates(salesDates, externalKeyToId.keys)

            // 거래처별 평균 — 양수 필터 + 양수 count divider (legacy 동등)
            val avgByAccountId: Map<Long, BigDecimal> = histories
                .groupBy { externalKeyToId[it.sapAccountCode] ?: 0L }
                .filter { it.key != 0L }
                .mapValues { (_, accountHistories) ->
                    val positives = accountHistories
                        .mapNotNull { it.abcClosingAmount1 }
                        .filter { it > BigDecimal.ZERO }
                    val divider = positives.size
                    if (divider > 0) {
                        positives
                            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
                            .divide(BigDecimal(divider), 0, RoundingMode.HALF_UP)
                    } else {
                        BigDecimal.ZERO
                    }
                }

            for (mfeis in chunk) {
                val accId = mfeis.account?.id
                if (accId == null) {
                    skippedRows++
                    continue
                }
                val newAmount = avgByAccountId[accId] ?: BigDecimal.ZERO
                val current = mfeis.thisMonthAmount
                val shouldUpdate = current == null || current.compareTo(newAmount) != 0
                if (!shouldUpdate) {
                    skippedRows++
                    continue
                }
                try {
                    saveThisMonthAmount(mfeis, newAmount)
                    updatedRows++
                } catch (ex: Exception) {
                    failedRows++
                    log.warn(
                        "MFEIS_THIS_MONTH_REVENUE_BATCH update failed id={} accountId={} reason={}",
                        mfeis.id, accId, ex.message
                    )
                }
            }
        }

        log.info(
            "MFEIS_THIS_MONTH_REVENUE_BATCH target={}-{} totalRows={} updated={} skipped={} failed={}",
            yearStr, monthStr, totalRows, updatedRows, skippedRows, failedRows
        )
        context?.metadata(
            mapOf(
                "yearMonth" to "$yearStr-$monthStr",
                "totalRows" to totalRows,
                "updatedRows" to updatedRows,
                "skippedRows" to skippedRows,
                "failedRows" to failedRows
            )
        )
    }

    @Transactional
    internal fun saveThisMonthAmount(mfeis: MonthlyFemaleEmployeeIntegrationSchedule, newAmount: BigDecimal) {
        mfeis.thisMonthAmount = newAmount
        mfeisRepository.save(mfeis)
    }
}
