package com.otoki.powersales.domain.foundation.account.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import java.time.LocalDate

/**
 * 폐업 거래처의 **등록/조회 면제** 정책 단일 출처.
 *
 * ## 면제 사유 (OR)
 * 1. `Distribution__c` 비어 있지 않음 — SF 정합
 * 2. `ABCTypeCode__c == 3062` — SF 정합
 * 3. **당월·전월 마감실적(> 0) 보유** — 신규 추가 (2026-08)
 *
 * 1·2 는 SF 레거시 원본에 있던 조건이다. 진열사원스케줄 거래처 lookupFilter
 * (`DisplayWorkScheduleMaster__c.Account__c`, booleanFilter `(1 OR 2) OR (3 AND 7) OR ((4 OR 5) AND 6)`)
 * 와 엑셀 업로드 검증 (`UplExcelBtnSchduleMasterController.cls:325-337`), 행사마스터 업로드 검증
 * (`UplExcelBtnPPTMasterController.cls:275`) 이 모두 이 두 조건으로 폐업 거래처를 통과시켰다.
 *
 * 3 은 신규 정책이다 — 폐업 처리됐더라도 최근 실매출이 잡힌 거래처는 행사/진열스케줄 등록 대상이
 * 될 수 있다 (SAP 상태 변경이 실거래 종료보다 선행하거나, 폐업 후 잔여 출고·정산이 남는 케이스).
 *
 * ## 두 소비처가 같은 기준을 쓰도록 여기로 모은다
 * - **조회**: 거래처 lookup 게이팅 (`AccountRepositoryCustomImpl.lookupGating`) — SQL predicate
 * - **등록 검증**: 진열사원스케줄 등록/수정 (`ScheduleUploadValidator`) — 사전 조회한 면제 id 집합
 *
 * 기준이 갈라지면 "화면 검색으로는 나오는데 등록하면 반려" (또는 그 반대) 가 된다. 실제로 신규 조회가
 * 1·2 면제 없이 폐업을 전건 배제하던 시기에 "엑셀로는 등록되는데 화면에서 못 찾는" 불일치가 있었다.
 *
 * ## 매출 존재 판정 축
 * 「월 매출(물류배부)」 화면이 실적으로 쓰는 `ClosingAmountSum` = `abc_closing_sum_amount +
 * ship_closing_sum_amount` (SF `ClosingAmountSum__c` formula 동등, null 은 BlankAsZero) **> 0**.
 * 개별 카테고리 컬럼(abc1~4 / ship1~4) 재합산은 합계 컬럼과 값이 항상 같지 않아 물류매출을
 * 누락시키므로 쓰지 않는다 (`MonthlySalesHistoryQueryGateway.closingAmountSum` 의 실측 근거 참조).
 */
object ClosedAccountSalesExemption {

    /** 거래처상태 picklist 의 폐업 값 — 이 상태일 때만 면제 판정이 의미를 갖는다. */
    const val ACCOUNT_STATUS_CLOSED = "폐업"

    /** SF 폐업 면제 대상 ABC유형코드 (`UplExcelBtnSchduleMasterController.cls:334` 등). */
    const val ABC_TYPE_CODE_EXEMPT = "3062"

    /**
     * 매출 이력을 보지 않고 판정할 수 있는 **SF 원본 면제** — `distribution` 비어 있지 않음 OR ABC유형 3062.
     *
     * 매출 조회가 필요한 3번 사유는 [com.otoki.powersales.domain.foundation.account.service.ClosedAccountSalesExemptionResolver]
     * 가, SQL 쪽 동등 조건은 `AccountRepositoryCustomImpl.closedAccountAttributeExemption` 이 담당한다.
     *
     * **빈 판정 축은 `isNullOrEmpty` (공백 문자열은 "값 있음")** — SF 원본 조건이 `Distribution__c
     * notEqual ""` 이고 SQL predicate 도 `distribution <> ''` 라, `isNullOrBlank` 를 쓰면 `"  "` 인
     * 거래처에서 조회(노출)와 등록(반려)이 갈린다. 이 정책이 없애려는 불일치가 바로 그 형태다.
     */
    fun isExemptByAccountAttributes(account: Account): Boolean =
        !account.distribution.isNullOrEmpty() || account.abcTypeCode == ABC_TYPE_CODE_EXEMPT

    /**
     * 예외 판정 대상 매출월 — [today] 기준 **당월 + 전월** 의 (년, 월) 쌍.
     *
     * 기준월은 행사/스케줄의 대상 기간이 아니라 **호출 시점의 시스템 현재월**이다 — 진입점마다
     * 기준월을 달리 넘기지 않아도 되고, 조회와 등록 검증이 동일하게 동작한다.
     *
     * `monthly_sales_history` 가 (`sales_year`, `sales_month`) picklist 2컬럼으로 매출월을 보유하므로
     * 쌍 단위로 반환한다 — 연말·연초 경계에서 전월이 전년도가 되는 케이스를 쌍 매칭으로 흡수하며,
     * 년 IN × 월 IN 의 cartesian 오매칭이 생기지 않는다.
     *
     * [SalesYear] picklist 는 2019~2030 범위라 그 밖의 시스템 시각에서는 **빈 목록**이 되고, 호출 측은
     * 예외를 무효화(폐업 전건 배제)해야 한다.
     */
    fun recentYearMonths(today: LocalDate = LocalDate.now()): List<Pair<SalesYear, SalesMonth>> =
        listOf(today, today.minusMonths(1)).mapNotNull { date ->
            val year = SalesYear.fromValueOrNull("%04d".format(date.year)) ?: return@mapNotNull null
            val month = SalesMonth.fromValueOrNull("%02d".format(date.monthValue)) ?: return@mapNotNull null
            year to month
        }
}
