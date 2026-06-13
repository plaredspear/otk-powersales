package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesResponse
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.domain.sales.repository.WorkingDayMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 월매출 조회 service.
 *
 * ## 레거시 매핑
 * - 진입점: 레거시 `POST /sales/monthlylistapi` → `promotion/month/list.jsp` (월 매출 현황)
 * - flow-legacy: SF Apex `IF_REST_MOBILE_MonthlySalesHistory.cls` 1회 호출이 화면 데이터 전부
 *   (Heroku PG mirror 조회 결과는 JSP 미참조) — 거래처(SAP 코드) 1곳 + 연월 기준
 *
 * ## 거래처 키 규약 (achieved=0 회귀 방지)
 * 모바일/물류매출 화면과 동일하게 `customerId` 는 **신규 내부 `account.id`** 를 전달받는다
 * ([LogisticsSalesService] 와 동일 규약). 숫자면 [AccountRepository] 로 [Account] 를 resolve 하고,
 * 숫자가 아니면 레거시 SAP 코드 직접 호출(관리/테스트) 로 보고 `externalKey` 로 resolve 한다.
 * **실적 조회는 resolve 된 `account.id` FK 로 수행한다** ([MonthlySalesHistoryQueryGateway.findBySalesDatesByAccountId]).
 * 레거시 SF Apex 는 Account 관계(`Account_ExternalKey__c` = `Account__r.Externalkey__c` formula) 로
 * 조인하는데, 신규 RDS `MonthlySalesHistory.account_id` 는 `account_sfid`(SF `AccountId__c`) → `account.sfid`
 * 로 resolve 된 안정적 FK 라 이와 동치다. 텍스트 컬럼 `sap_account_code`(SF `SAPAccountCode__c`) 는
 * 적재 품질(과거 row null/불일치 가능)에 의존하므로 조인 키로 쓰지 않는다.
 *
 * ## 데이터 source
 * - 실적: RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제) 실측 마감실적 — `account_id` FK 조회
 *   ([MonthlySalesHistoryQueryGateway] 경유).
 * - 목표: RDS `SalesProgressRateMaster` (SF `SalesProgressRateMaster__c` 복제) — 조회 거래처의
 *   해당 (연, 월) 1행. SF Apex SOQL `TargetYear__c = year AND TargetMonth__c = month AND
 *   Account__r.ExternalKey__c = SAPAccountCode` 정합.
 *
 * ## 응답 산출 (레거시 list.jsp 정합)
 * - `achievedAmount` = 조회월 `ClosingAmountSum` (ABC합 + Ship합) — "마감 합계 실적"
 * - `targetAmount` = `RT + FR + RM + FO` 목표 합계 (SF `TargetSum__c` 동등) — "목표 금액"
 * - `achievementRate` = `round(achieved / targetSum * 100)` — 진도율 바 + "(N% 달성)".
 *   목표 미등록(targetSum=0) 월은 0 (레거시 `isNoTargetMonth` 분기 정합). 레거시 SF `ProgressRate__c`
 *   (= `CurrentMonthSalesAmount__c / TargetSum__c`) 와 `CurrentMonthSalesAmount == ClosingAmountSum`
 *   인 정상 케이스에서 동등하며, 화면 "(N% 달성)" 표기(실적÷목표) 와도 일치.
 * - `categorySales` = 카테고리별 목표(RT/RM/FR/FO) + 실적(ABC+Ship) + 달성률(round(실적÷목표×100))
 * - `baseRate` = 기준 진도율(영업일 경과 비율). 레거시 `calcBusinessRateOnlyThisMonth` 동등 — 상세는 [baseRate]
 * - `yearComparison` = 당년 / 전년 동월 ClosingAmountSum
 * - `monthlyAverage` = 1월~조회월 누적 ClosingAmountSum / 월수 (당년 / 전년)
 */
@Service
@Transactional(readOnly = true)
class MonthlySalesService(
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository,
    private val workingDayMasterRepository: WorkingDayMasterRepository,
) {

    fun getMonthlySales(request: MonthlySalesRequest): MonthlySalesResponse {
        val customerIdRaw = request.customerId
        val year = request.getYear()
        val month = request.getMonth()
        if (customerIdRaw.isNullOrBlank()) {
            return emptyResponse("ALL", request.yearMonth, month)
        }

        // customerId(=내부 account.id) → Account resolve. 숫자가 아니면 레거시 SAP 코드 직접 호출(관리/테스트).
        val account = customerIdRaw.toLongOrNull()
            ?.let { accountRepository.findByIdInAndIsDeletedNot(listOf(it), true).firstOrNull() }
            ?: accountRepository.findByExternalKey(customerIdRaw)
        val customerName = account?.name ?: customerIdRaw

        // 실적: account_id FK 로 조회 (레거시 Account 관계 조인 정합). RDS `MonthlySalesHistory.account_id`
        // 는 `account_sfid`(SF `AccountId__c`) → `account.sfid` 로 resolve 된 안정적 FK 라, 텍스트 컬럼
        // `sap_account_code`(SF `SAPAccountCode__c`) 적재 품질과 무관하게 매칭된다. account 미resolve 시
        // 실적 없음 (전체 0) — baseRate 는 거래처와 무관하게 아래에서 그대로 산출.
        val currentRangeSalesDates = (1..month).map { toSalesDate(year, it) }
        val previousRangeSalesDates = (1..month).map { toSalesDate(year - 1, it) }
        val rowsByDate = account
            ?.let {
                monthlySalesHistoryGateway
                    .findBySalesDatesByAccountId(
                        (currentRangeSalesDates + previousRangeSalesDates).distinct(),
                        listOf(it.id),
                    )
                    .associateBy { row -> row.salesDate }
            }
            ?: emptyMap()

        val currentRow = rowsByDate[toSalesDate(year, month)]
        val previousRow = rowsByDate[toSalesDate(year - 1, month)]

        val achieved = currentRow?.closingAmountSum?.toLong() ?: 0L
        val previousAchieved = previousRow?.closingAmountSum?.toLong() ?: 0L

        val currentAvg = currentRangeSalesDates
            .sumOf { rowsByDate[it]?.closingAmountSum?.toLong() ?: 0L } / month
        val previousAvg = previousRangeSalesDates
            .sumOf { rowsByDate[it]?.closingAmountSum?.toLong() ?: 0L } / month

        // 목표: 조회 거래처의 (연, 월) 1행. account 미resolve 시 목표 없음 (전체 0).
        val target = account?.let { findTarget(it.id, year, month) }
        val targetSum = target?.let { targetSumOf(it) } ?: 0L

        return MonthlySalesResponse(
            customerId = customerIdRaw,
            customerName = customerName,
            yearMonth = request.yearMonth,
            targetAmount = targetSum,
            achievedAmount = achieved,
            achievementRate = rate(achieved, targetSum),
            baseRate = baseRate(year, month),
            categorySales = buildCategorySales(currentRow, target),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(achieved, previousAchieved),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(
                currentYearAverage = currentAvg,
                previousYearAverage = previousAvg,
                startMonth = 1,
                endMonth = month,
            ),
        )
    }

    private fun emptyResponse(customerId: String, yearMonth: String, month: Int) =
        MonthlySalesResponse(
            customerId = customerId,
            customerName = customerId,
            yearMonth = yearMonth,
            targetAmount = 0L,
            achievedAmount = 0L,
            achievementRate = 0.0,
            baseRate = 0.0,
            categorySales = emptyList(),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(0L, 0L),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(0L, 0L, 1, month),
        )

    /**
     * 조회 거래처의 (연, 월) 목표 1행. `targetYear` 는 `"YYYY"` 문자열, `targetMonth` 는 SF 적재
     * 포맷(zero-pad 비보장) 이라 정수 파싱 후 월 일치로 매칭. soft-delete row 제외.
     */
    private fun findTarget(accountId: Long, year: Int, month: Int): SalesProgressRateMaster? =
        salesProgressRateMasterRepository
            .findByAccountIdAndTargetYear(accountId, year.toString())
            .asSequence()
            .filter { it.isDeleted != true }
            .firstOrNull { it.targetMonth?.trim()?.toIntOrNull() == month }

    private fun buildCategorySales(
        oro: MonthlySalesRow?,
        target: SalesProgressRateMaster?,
    ): List<MonthlySalesResponse.CategorySalesInfo> {
        if (oro == null && target == null) return emptyList()
        return SalesCategory.entries.map { category ->
            val achievedAmount = oro?.let { categoryAchieved(it, category) } ?: 0L
            val targetAmount = target?.let { categoryTarget(it, category) } ?: 0L
            MonthlySalesResponse.CategorySalesInfo(
                category = category.name,
                targetAmount = targetAmount,
                achievedAmount = achievedAmount,
                achievementRate = rate(achievedAmount, targetAmount),
            )
        }
    }

    private fun categoryAchieved(oro: MonthlySalesRow, category: SalesCategory): Long {
        val abc = when (category) {
            SalesCategory.AMBIENT -> oro.abcClosingAmount1
            SalesCategory.NOODLE -> oro.abcClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.abcClosingAmount3
            SalesCategory.OIL_FAT -> oro.abcClosingAmount4
        }
        val ship = when (category) {
            SalesCategory.AMBIENT -> oro.shipClosingAmount1
            SalesCategory.NOODLE -> oro.shipClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.shipClosingAmount3
            SalesCategory.OIL_FAT -> oro.shipClosingAmount4
        }
        return (abc ?: BigDecimal.ZERO).toLong() + (ship ?: BigDecimal.ZERO).toLong()
    }

    /** 카테고리별 목표 — SF `RT/RM/FR/FO TargetAmount__c` 정합. */
    private fun categoryTarget(target: SalesProgressRateMaster, category: SalesCategory): Long {
        val amount = when (category) {
            SalesCategory.AMBIENT -> target.rtTargetAmount
            SalesCategory.NOODLE -> target.rmTargetAmount
            SalesCategory.FROZEN_REFRIGERATED -> target.frTargetAmount
            SalesCategory.OIL_FAT -> target.foTargetAmount
        }
        return amount?.toLong() ?: 0L
    }

    /** 목표 합계 — SF `TargetSum__c` (= RT + FR + RM + FO) 동등. */
    private fun targetSumOf(target: SalesProgressRateMaster): Long =
        ((target.rtTargetAmount ?: 0.0) +
            (target.frTargetAmount ?: 0.0) +
            (target.rmTargetAmount ?: 0.0) +
            (target.foTargetAmount ?: 0.0)).toLong()

    /** 달성률 — `round(실적 / 목표 × 100)`. 목표 0 이면 0 (NaN/Infinity 방지, 레거시 정합). */
    private fun rate(achieved: Long, target: Long): Double =
        if (target <= 0L) 0.0 else Math.round(achieved.toDouble() / target * 100).toDouble()

    /**
     * 기준 진도율 (영업일 기준, %) — 레거시 SF `calcBusinessRateOnlyThisMonth` **byte 단위 정합**.
     *
     * 조회월이 시스템 당월일 때만 `(월초~오늘 영업일) / (월초~월말 영업일) × 100`, 그 외(과거/미래)
     * 월은 0 (레거시 JSP 동일 — `MonthlySalesAdminQueryService.referenceAchievementRate` 의 admin
     * 정책(과거 100) 과 다름).
     *
     * **영업일 source = `WorkingDayMaster`** (SF `WorkingDayMaster__c` 복제) 의 `workingDateCheck = 1`
     * row 를 기간 count — 레거시 SF SOQL 과 동일 소스/조건. 평일·공휴일을 코드로 유추하지 않고 운영
     * 영업일 달력 그대로 사용하므로 토요일 영업일 지정 등 운영 규칙까지 정확히 반영된다.
     * 마스터 미적재(빈 테이블) 시 `total <= 0` → 0 반환 (graceful, 레거시 `fromEndDayCnt <= 0` 정합).
     */
    private fun baseRate(year: Int, month: Int): Double {
        val today = LocalDate.now()
        if (today.year != year || today.monthValue != month) return 0.0
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        val totalWorkingDays = workingDayMasterRepository.countWorkingDays(firstDay, lastDay, WORKING_DAY_CHECK)
        if (totalWorkingDays <= 0L) return 0.0
        val elapsedWorkingDays = workingDayMasterRepository.countWorkingDays(firstDay, today, WORKING_DAY_CHECK)
        return elapsedWorkingDays.toDouble() / totalWorkingDays.toDouble() * 100.0
    }

    private fun toSalesDate(year: Int, month: Int): String = "%04d%02d".format(year, month)

    private companion object {
        /** SF `WorkingDateCheck__c` 의 영업일 값. */
        const val WORKING_DAY_CHECK = 1.0
    }
}
