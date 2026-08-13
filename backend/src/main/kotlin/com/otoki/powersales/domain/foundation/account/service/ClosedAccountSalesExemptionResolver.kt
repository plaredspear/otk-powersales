package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.policy.ClosedAccountSalesExemption
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 폐업 거래처 중 **최근 매출 예외**로 등록이 허용되는 대상을 판별한다.
 *
 * 거래처 lookup 은 같은 판정을 EXISTS 서브쿼리로 쿼리 안에서 수행하지만
 * ([com.otoki.powersales.domain.foundation.account.repository.AccountRepositoryCustomImpl] 의
 * `recentSalesExists`), 진열사원스케줄 **등록 검증**을 담당하는
 * [com.otoki.powersales.domain.activity.schedule.service.ScheduleUploadValidator] 는 DB 에 접근하지
 * 않는 순수 컴포넌트다. 그래서 호출 측이 본 resolver 로 면제 대상 거래처 id 집합을 미리 뽑아 검증기에
 * 주입한다 — 엑셀 업로드는 행이 수백 건이라 행별 조회 대신 일괄 조회 1회로 끝낸다.
 *
 * 기준월·판정 축은 [ClosedAccountSalesExemption] 이 단일 출처이므로 조회와 등록의 판정이 갈라지지 않는다.
 */
@Component
@Transactional(readOnly = true)
class ClosedAccountSalesExemptionResolver(
    private val monthlySalesHistoryQueryGateway: MonthlySalesHistoryQueryGateway,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * [accounts] 중 **폐업 상태이면서 당월·전월 마감실적(> 0) 을 보유한** 거래처의 id 집합.
     *
     * 조회 모수는 매출을 봐야만 판정할 수 있는 거래처로 좁힌다 — 비폐업이거나 SF 원본 면제
     * ([ClosedAccountSalesExemption.isExemptByAccountAttributes]) 에 이미 해당하면 매출과 무관하게
     * 통과하므로 조회할 이유가 없다. 호출 측은 본 결과와 SF 원본 면제를 OR 로 합쳐 판정한다.
     *
     * [ClosedAccountSalesExemption.recentYearMonths] 가 빈 목록이면 (SalesYear picklist 범위 밖 시각)
     * 매출 면제가 무효화되어 빈 집합을 반환한다 — SF 원본 면제는 호출 측에서 그대로 살아 있다.
     */
    fun resolveExemptedAccountIds(accounts: Collection<Account>): Set<Long> {
        val closedAccountIds = accounts
            .filter { it.accountStatusName == ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED }
            .filterNot { ClosedAccountSalesExemption.isExemptByAccountAttributes(it) }
            .map { it.id }
            .toSet()
        if (closedAccountIds.isEmpty()) return emptySet()

        val yearMonths = ClosedAccountSalesExemption.recentYearMonths()
        if (yearMonths.isEmpty()) {
            // 조회 측(AccountRepositoryCustomImpl.recentSalesExists) 과 동일 사유·동일 관측 수준 유지.
            log.warn("SalesYear picklist 범위 밖 시스템 시각 — 폐업 거래처 최근 매출 면제가 무효화됨 (등록 검증)")
            return emptySet()
        }

        // 게이트웨이는 `YYYYMM` 문자열 목록을 받아 (년, 월) 쌍 집합으로 정확 매칭하고 soft-delete row 를
        // 걸러낸다 — 쌍 매칭이라 연말·연초 경계의 cartesian 오매칭이 없다.
        val salesDates = yearMonths.map { (year, month) -> year.value + month.value }

        return monthlySalesHistoryQueryGateway
            .findBySalesDatesByAccountId(salesDates, closedAccountIds)
            .filter { it.closingAmountSum > BigDecimal.ZERO }
            .mapNotNull { it.accountId }
            .toSet()
    }
}
