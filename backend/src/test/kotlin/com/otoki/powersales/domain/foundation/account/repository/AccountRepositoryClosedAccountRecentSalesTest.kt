package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.querydsl.core.types.dsl.Expressions
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

/**
 * 행사마스터 / 진열사원스케줄 마스터 거래처 lookup 의 **폐업 거래처 최근 매출 예외** 검증.
 *
 * `excludeClosedAccount = true` 는 폐업 거래처를 배제하되, 조회 시점 기준 당월·전월에 마감실적
 * (`ClosingAmountSum` = `abc_closing_sum_amount + ship_closing_sum_amount` > 0) 이 있는 거래처는
 * 예외로 노출한다 ([AccountRepositoryCustomImpl.notClosedOrHasRecentSales]).
 *
 * 판정 축은 「월 매출(물류배부)」 화면이 실적으로 쓰는 값과 동일하게 **합계 컬럼 2개**이며, 개별
 * 카테고리 컬럼(abc1~4 / ship1~4) 은 판정에 쓰지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository lookup — 폐업 거래처 당월·전월 매출 예외 노출")
class AccountRepositoryClosedAccountRecentSalesTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val allowAll = Expressions.asBoolean(true).isTrue

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistAccount(
        name: String,
        externalKey: String,
        accountStatusName: String,
        distribution: String? = null,
    ): Account {
        val saved = testEntityManager.persistAndFlush(
            Account(
                name = name,
                externalKey = externalKey,
                accountGroup = "1000",
                accountStatusName = accountStatusName,
                distribution = distribution,
                isDeleted = false,
            )
        )
        testEntityManager.clear()
        return saved
    }

    /** [monthsAgo] 개월 전 매출월의 마감실적 row 적재. 합계 컬럼(abc/ship) 을 직접 지정한다. */
    private fun persistSales(
        account: Account,
        monthsAgo: Long,
        abcSum: Double? = null,
        shipSum: Double? = null,
        isDeleted: Boolean? = false,
    ) {
        val date = LocalDate.now().minusMonths(monthsAgo)
        val managed = testEntityManager.find(Account::class.java, account.id)
        testEntityManager.persistAndFlush(
            MonthlySalesHistory(
                salesYear = SalesYear.fromValueOrNull("%04d".format(date.year)),
                salesMonth = SalesMonth.fromValueOrNull("%02d".format(date.monthValue)),
                abcClosingSumAmount = abcSum,
                shipClosingSumAmount = shipSum,
                isDeleted = isDeleted,
                account = managed,
            )
        )
        testEntityManager.clear()
    }

    private fun lookup(accountStatusName: String? = null) = accountRepository.findAllAccessibleByPolicy(
        policyPredicate = allowAll,
        keyword = null,
        abcType = null,
        accountType = null,
        accountStatusName = accountStatusName,
        applyPromotionFilter = true,
        excludeClosedAccount = true,
        coordinatesMissing = false,
        pageable = PageRequest.of(0, 20),
    )

    @Test
    @DisplayName("매출 없는 폐업 거래처는 제외 (기존 동작 유지)")
    fun excludesClosedAccountWithoutSales() {
        val closed = persistAccount("폐점 A", "CL-1", accountStatusName = "폐업")

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("distribution 보유 폐업 거래처도 매출 없으면 제외 (excludeClosedAccount 가 면제보다 우선)")
    fun excludesClosedAccountWithDistributionButNoSales() {
        val closed = persistAccount("폐점 A2", "CL-1B", accountStatusName = "폐업", distribution = "10")

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("당월 매출이 있는 폐업 거래처는 노출")
    fun includesClosedAccountWithCurrentMonthSales() {
        val closed = persistAccount("폐점 B", "CL-2", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = 1_000_000.0)

        assertThat(lookup().content.map { it.id }).contains(closed.id)
    }

    @Test
    @DisplayName("전월 매출이 있는 폐업 거래처는 노출")
    fun includesClosedAccountWithLastMonthSales() {
        val closed = persistAccount("폐점 C", "CL-3", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 1, shipSum = 500_000.0)

        assertThat(lookup().content.map { it.id }).contains(closed.id)
    }

    @Test
    @DisplayName("물류 합계 컬럼(ship_closing_sum_amount) 단독으로도 매출 존재로 인정")
    fun includesClosedAccountWithShipSumOnly() {
        val closed = persistAccount("폐점 D", "CL-4", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = null, shipSum = 300_000.0)

        assertThat(lookup().content.map { it.id }).contains(closed.id)
    }

    @Test
    @DisplayName("2개월 전 매출만 있는 폐업 거래처는 제외 (당월·전월 한정)")
    fun excludesClosedAccountWithOlderSalesOnly() {
        val closed = persistAccount("폐점 E", "CL-5", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 2, abcSum = 9_000_000.0)

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("합계가 0 인 매출 row 는 매출 없음으로 판정 — 제외")
    fun excludesClosedAccountWithZeroAmount() {
        val closed = persistAccount("폐점 F", "CL-6", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = 0.0, shipSum = 0.0)

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("합계가 음수(반품·조정) 인 매출 row 는 매출 없음으로 판정 — 제외")
    fun excludesClosedAccountWithNegativeAmount() {
        val closed = persistAccount("폐점 G", "CL-7", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = -200_000.0)

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("soft-delete 된 매출 row 는 판정에서 제외")
    fun excludesClosedAccountWithSoftDeletedSales() {
        val closed = persistAccount("폐점 H", "CL-8", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = 1_000_000.0, isDeleted = true)

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("다른 거래처의 매출로는 노출되지 않음 (account_id 매칭)")
    fun doesNotLeakAcrossAccounts() {
        val closed = persistAccount("폐점 I", "CL-9", accountStatusName = "폐업")
        val active = persistAccount("영업중 J", "AC-9", accountStatusName = "거래")
        persistSales(active, monthsAgo = 0, abcSum = 8_000_000.0)

        assertThat(lookup().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("비폐업 거래처는 매출 유무와 무관하게 노출 (기존 동작 유지)")
    fun includesActiveAccountRegardlessOfSales() {
        val active = persistAccount("영업중 K", "AC-10", accountStatusName = "거래")

        assertThat(lookup().content.map { it.id }).contains(active.id)
    }

    /**
     * 물류 클레임(`/lookup-for-claim`) / 유통기한·재고조회(`/lookup-for-product`) lookup 경로 —
     * `applyPromotionFilter = true` + `excludeClosedAccount = false`.
     *
     * 매출 예외는 행사마스터 / 진열사원스케줄 2개 화면 한정이므로, 이 경로는 SF `AccId__c.lookupFilter`
     * 원본(폐업 배제 + distribution 면제) 동작을 그대로 유지해야 한다.
     */
    private fun lookupWithoutClosedExclusion() = accountRepository.findAllAccessibleByPolicy(
        policyPredicate = allowAll,
        keyword = null,
        abcType = null,
        accountType = null,
        accountStatusName = null,
        applyPromotionFilter = true,
        excludeClosedAccount = false,
        coordinatesMissing = false,
        pageable = PageRequest.of(0, 20),
    )

    @Test
    @DisplayName("클레임/제품 lookup 경로 — 매출이 있어도 폐업 거래처는 노출되지 않음 (종전 동작 유지)")
    fun doesNotApplyRecentSalesExceptionWhenClosedExclusionOff() {
        val closed = persistAccount("폐점 R", "CL-15", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = 1_000_000.0)

        assertThat(lookupWithoutClosedExclusion().content.map { it.id }).doesNotContain(closed.id)
    }

    @Test
    @DisplayName("클레임/제품 lookup 경로 — distribution 보유 폐업 거래처는 종전대로 노출 (면제 유지)")
    fun keepsDistributionExemptionWhenClosedExclusionOff() {
        val closed = persistAccount("폐점 S", "CL-16", accountStatusName = "폐업", distribution = "10")

        assertThat(lookupWithoutClosedExclusion().content.map { it.id }).contains(closed.id)
    }

    @Test
    @DisplayName("거래상태 드롭다운 distinct — 매출 예외 폐업 거래처가 있으면 '폐업' 노출")
    fun exposesClosedInFilterOptionsWhenExceptionExists() {
        val closed = persistAccount("폐점 L", "CL-11", accountStatusName = "폐업")
        persistSales(closed, monthsAgo = 0, abcSum = 1_000_000.0)
        persistAccount("영업중 M", "AC-11", accountStatusName = "거래")

        assertThat(accountRepository.findDistinctAccountStatusNames(allowAll))
            .containsExactlyInAnyOrder("거래", "폐업")
    }

    @Test
    @DisplayName("거래상태 드롭다운 distinct — 매출 예외가 없으면 '폐업' 미노출 (기존 동작 유지)")
    fun hidesClosedInFilterOptionsWithoutException() {
        persistAccount("폐점 N", "CL-12", accountStatusName = "폐업")
        persistAccount("영업중 O", "AC-12", accountStatusName = "거래")

        assertThat(accountRepository.findDistinctAccountStatusNames(allowAll))
            .containsExactly("거래")
    }

    @Test
    @DisplayName("거래상태=폐업 필터 조회 시 매출 예외 거래처만 반환")
    fun filtersByClosedStatusReturnsOnlyExceptions() {
        val exempt = persistAccount("폐점 P", "CL-13", accountStatusName = "폐업")
        persistSales(exempt, monthsAgo = 1, shipSum = 700_000.0)
        persistAccount("폐점 Q", "CL-14", accountStatusName = "폐업")

        assertThat(lookup(accountStatusName = "폐업").content.map { it.id })
            .containsExactly(exempt.id)
    }
}
