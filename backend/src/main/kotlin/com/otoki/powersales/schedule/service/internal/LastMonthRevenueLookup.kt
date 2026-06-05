package com.otoki.powersales.schedule.service.internal

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * `DisplayWorkSchedule.lastMonthRevenue` 적재용 직전월 매출 lookup helper.
 *
 * ## SF 레거시 정합 source
 *
 * SF `UpdateLastMonthRevenueBatch.cls:34, 58` 의 `MonthlySalesHistory__c.ClosingAmountSum__c`
 * (= formula `ABCClosingSumAmount__c + ShipClosingSumAmount__c`) 동등. 신규 시스템은 SF 와 동일하게
 * RDS `MonthlySalesHistory` (`MonthlySalesHistory__c` 복제 적재) 의
 * `(abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)` 합산
 * ([com.otoki.powersales.sales.service.MonthlySalesRow.closingAmountSum]) 으로 동등 산출.
 *
 * SF batch / Trigger 의 fallback 정합: row 부재 시 `0` ([UpdateLastMonthRevenueBatch.cls:56-60],
 * [DisplayWorkScheduleMasterTriggerHandler.setLastMonthRevenue:288-292]).
 *
 * 단건 변형 [forAccount] 는 SF 정합과 별개로 신규 시스템의 호출 흐름 (`AdminScheduleService` 단건
 * createSchedule / updateSchedule) 편의를 위해 `null` 반환 — 호출 측이 `?:` 로 fallback 선택 가능.
 *
 * ## 호출 흐름
 * - 일괄 ([forAccounts]) — confirmUpload (엑셀 import) + [DisplayMasterLastMonthRevenueBatchService] daily batch
 * - 단건 ([forAccount]) — createSchedule / updateSchedule
 *
 * ## 변환 계층
 * - 입력: `List<Account>` (또는 단건 `Account?`) — `Account.externalKey` = SAP 거래처 코드
 * - 직전월 산출: `today.minusMonths(1)` → `"YYYYMM"` 6자 문자열 (게이트웨이가 `SalesYear`/`SalesMonth` 로 변환)
 * - 호출: [MonthlySalesHistoryQueryGateway.findBySalesDates]
 * - 결과 매핑: `sapAccountCode → Account.id` 재매핑 후 `Map<Long, BigDecimal>` 반환
 *
 * SF `LastMonthRevenue__c` 의 `Number(precision=18, scale=0)` 정합 보존 — `setScale(0, HALF_UP)`
 * 로 소수점 절단.
 */
@Component
class LastMonthRevenueLookup(
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
) {

    /**
     * 일괄 lookup — `accounts` 전체의 직전월 매출을 `account.id → revenue` Map 으로 반환.
     *
     * SF batch 정합 — row 부재 거래처는 entry 부재 (호출 측에서 `map[id] ?: BigDecimal.ZERO` null-safe
     * 인용 가능). `accounts.isEmpty()` 또는 `externalKey == null` 인 거래처는 결과에서 제외.
     */
    fun forAccounts(
        accounts: List<Account>,
        today: LocalDate = LocalDate.now(),
    ): Map<Long, BigDecimal> {
        if (accounts.isEmpty()) return emptyMap()
        val externalKeyToId: Map<String, Long> = accounts
            .filter { it.externalKey != null }
            .associate { it.externalKey!! to it.id }
        if (externalKeyToId.isEmpty()) return emptyMap()
        val salesDate = previousMonthYyyymm(today)
        return monthlySalesHistoryGateway
            .findBySalesDates(listOf(salesDate), externalKeyToId.keys)
            .mapNotNull { row ->
                val accountId = externalKeyToId[row.sapAccountCode] ?: return@mapNotNull null
                accountId to row.closingAmountSum.setScale(0, RoundingMode.HALF_UP)
            }
            .toMap()
    }

    /**
     * 단건 lookup — 단일 `account` 의 직전월 매출 반환.
     *
     * `account == null` 또는 `externalKey == null` 또는 매출 row 부재 시 `null` 반환.
     * (SF batch 의 fallback `0` 과 다름 — 단건 변형은 호출 측 fallback 선택권 부여)
     */
    fun forAccount(
        account: Account?,
        today: LocalDate = LocalDate.now(),
    ): BigDecimal? {
        val externalKey = account?.externalKey ?: return null
        val salesDate = previousMonthYyyymm(today)
        return monthlySalesHistoryGateway
            .findBySalesDates(listOf(salesDate), listOf(externalKey))
            .firstOrNull()
            ?.closingAmountSum
            ?.setScale(0, RoundingMode.HALF_UP)
    }

    private fun previousMonthYyyymm(today: LocalDate): String {
        val lastMonth = today.minusMonths(1)
        return "%d%02d".format(lastMonth.year, lastMonth.monthValue)
    }
}
