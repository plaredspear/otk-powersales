package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.domain.sales.dto.response.LogisticsSalesResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 물류매출 조회 service.
 *
 * ## 레거시 매핑
 * - 진입점: 레거시 `GET /sales/monthList` → `promotion/month/list.jsp` (월 매출 조회/물류)
 * - flow-legacy: `PromotionController.monthList` → `accountMapper.selectMonthlyCurrentYear`
 *   (SF `MonthlySalesHistory__c` mirror) — 거래처+연월별 온도대 마감실적 조회
 * - origin: heroku→mobile gap inventory §6 P4 (물류매출), spec #823(ORORA datasource) 연계
 *
 * ## 레거시 동작 요약
 * - 입력: 거래처(`SAPAccountCode`) 1곳 + 연(`year`) + 월(`month`)
 * - 분기: 당월/이전월 구분 표시 (당월=진행중, 이전월=마감)
 * - 외부 호출: SF Apex `IF_REST_MOBILE_MonthlySalesHistory` 호출 후 PostgreSQL mirror 로 덮어씀
 * - 부수효과: 없음 (순수 조회)
 *
 * ## 데이터 source (신규)
 * RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) 의 `shipClosingAmount1~3`
 * (물류마감실적 — 상온/라면/냉장냉동) 실측만 사용. [MonthlySalesHistoryQueryGateway] 경유.
 * 거래처 코드는 [com.otoki.powersales.domain.foundation.account.entity.Account.externalKey] 를
 * `sapAccountCode` 키로 그대로 사용 ([MonthlySalesAdminQueryService] 와 동일 — 000 정규화 불필요).
 *
 * ## 응답 산출
 * - 온도대별 `currentAmount` = 조회월 ShipClosing, `previousYearAmount` = 전년 동월 ShipClosing
 * - `difference` = current - previousYear, `growthRate` = 전년 대비 증감율(%) (전년 0 이면 0.0)
 * - `isCurrentMonth` = 조회 연월 == 시스템 당월
 *
 * ## 신규 차이 (deviation)
 * - **목표/진도율 폐기**: `MonthlySalesHistory` 목표 테이블 드롭으로 영구 폐기 (월매출 조회와 동일 정책)
 * - **`shipClosingAmount4`(유지) 미포함**: 모바일 물류매출 화면이 온도대 3종 탭(상온/라면/냉동·냉장)만
 *   보유 → 레거시 4종 중 유지류는 현 모바일 UI 범위 밖이라 반환 제외 ([LogisticsSalesResponse] 참조)
 * - **SF→RDS 직조회**: 레거시 SF Apex 경유 대신 RDS `MonthlySalesHistory` 직접 SELECT
 */
@Service
@Transactional(readOnly = true)
class LogisticsSalesService(
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
) {

    /**
     * 온도대 → RDS ShipClosing 컬럼 매핑. 모바일 `LogisticsCategory` 코드와 정합.
     */
    private enum class LogisticsBand(
        val code: String,
        val shipAmount: (MonthlySalesRow) -> BigDecimal?
    ) {
        NORMAL("NORMAL", { it.shipClosingAmount1 }),
        RAMEN("RAMEN", { it.shipClosingAmount2 }),
        FROZEN("FROZEN", { it.shipClosingAmount3 }),
    }

    fun getLogisticsSales(customerId: Long, yearMonth: String): LogisticsSalesResponse {
        val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
            ?: throw BusinessException(
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "거래처를 찾을 수 없습니다: $customerId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

        val year = yearMonth.substring(0, 4).toInt()
        val month = yearMonth.substring(4, 6).toInt()
        val currentSalesDate = yearMonth
        val previousSalesDate = "%04d%02d".format(year - 1, month)
        val isCurrentMonth = yearMonth == currentYearMonth()

        val sapCode = account.externalKey
        val rowsByDate = if (sapCode.isNullOrBlank()) {
            emptyMap()
        } else {
            monthlySalesHistoryGateway
                .findBySalesDates(listOf(currentSalesDate, previousSalesDate), listOf(sapCode))
                .associateBy { it.salesDate }
        }
        val currentRow = rowsByDate[currentSalesDate]
        val previousRow = rowsByDate[previousSalesDate]

        val categories = LogisticsBand.entries.map { band ->
            val current = shipAmountOf(currentRow, band)
            val previous = shipAmountOf(previousRow, band)
            val difference = current - previous
            LogisticsSalesResponse.CategorySales(
                category = band.code,
                currentAmount = current,
                previousYearAmount = previous,
                difference = difference,
                growthRate = if (previous == 0L) 0.0 else (difference.toDouble() / previous) * 100.0,
            )
        }

        return LogisticsSalesResponse(
            customerId = account.id,
            customerName = account.name ?: "",
            sapAccountCode = sapCode ?: "",
            yearMonth = yearMonth,
            isCurrentMonth = isCurrentMonth,
            categories = categories,
        )
    }

    private fun shipAmountOf(row: MonthlySalesRow?, band: LogisticsBand): Long =
        if (row == null) 0L else (band.shipAmount(row) ?: BigDecimal.ZERO).toLong()

    private fun currentYearMonth(): String =
        LocalDate.now().let { "%04d%02d".format(it.year, it.monthValue) }
}
