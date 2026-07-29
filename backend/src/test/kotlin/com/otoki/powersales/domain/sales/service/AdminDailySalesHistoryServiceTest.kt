package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import com.otoki.powersales.domain.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("AdminDailySalesHistoryService 테스트")
class AdminDailySalesHistoryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val dailySalesHistoryRepository: DailySalesHistoryRepository = mockk()
    private val service = AdminDailySalesHistoryService(accountRepository, dailySalesHistoryRepository)

    private val allBranchScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    private fun account(
        externalKey: String = "1000000",
        branchCode: String? = "1100",
    ): Account = Account(
        id = 1,
        name = "GS25 역삼점",
        externalKey = externalKey,
        branchCode = branchCode,
        branchName = "강남지점",
    )

    private fun daily(
        salesDate: String,
        erpSales: Double? = null,
        erpDistribution: Double? = null,
        ledger: Double? = null,
        updatedAt: LocalDateTime = LocalDateTime.of(2026, 7, 31, 11, 0),
    ): DailySalesHistory = DailySalesHistory(
        sapAccountCode = "1000000",
        salesDate = salesDate,
        externalKey = "1000000$salesDate",
    ).also {
        it.erpSalesAmount = erpSales
        it.erpDistributionAmount = erpDistribution
        it.ledgerAmount = ledger
        it.updatedAt = updatedAt
    }

    @Test
    @DisplayName("거래처코드 + 매출월로 일별 행을 조회하고 금액 3종을 합산한다")
    fun returnsRowsWithTotals() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc("1000000", "202607")
        } returns listOf(
            daily("20260731", erpSales = 1000.0, erpDistribution = 200.0, ledger = 50.0),
            daily("20260731", erpSales = 500.0, erpDistribution = null, ledger = null),
        )

        val response = service.getDailySalesHistories(allBranchScope, "1000000", "202607")

        assertThat(response.salesMonth).isEqualTo("202607")
        assertThat(response.sapAccountCode).isEqualTo("1000000")
        assertThat(response.accountName).isEqualTo("GS25 역삼점")
        assertThat(response.branchName).isEqualTo("강남지점")
        assertThat(response.content).hasSize(2)
        assertThat(response.totalErpSalesAmount).isEqualTo(1500.0)
        assertThat(response.totalErpDistributionAmount).isEqualTo(200.0)
        assertThat(response.totalLedgerAmount).isEqualTo(50.0)
    }

    @Test
    @DisplayName("마지막 적재 시각은 조회한 거래처+월 행의 max(updatedAt), 결과 0건이면 null")
    fun returnsLastMaterializedAtOfQueriedMonth() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(any(), any())
        } returns listOf(
            daily("20260730", updatedAt = LocalDateTime.of(2026, 7, 30, 11, 0)),
            daily("20260731", updatedAt = LocalDateTime.of(2026, 7, 31, 11, 5)),
        )

        assertThat(service.getDailySalesHistories(allBranchScope, "1000000", "202607").lastMaterializedAt)
            .isEqualTo(LocalDateTime.of(2026, 7, 31, 11, 5))

        every {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(any(), any())
        } returns emptyList()

        assertThat(service.getDailySalesHistories(allBranchScope, "1000000", "202605").lastMaterializedAt)
            .isNull()
    }

    @Test
    @DisplayName("매출월에 하이픈이 섞여 있어도 yyyyMM 으로 정규화해 조회한다")
    fun normalizesHyphenatedSalesMonth() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(any(), any())
        } returns emptyList()

        val response = service.getDailySalesHistories(allBranchScope, "1000000", "2026-07")

        assertThat(response.salesMonth).isEqualTo("202607")
        verify {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc("1000000", "202607")
        }
    }

    @Test
    @DisplayName("ORORA 원본 형식(선행 000) 거래처코드는 prefix 를 떼고 재조회한다")
    fun stripsOroraAccountCodePrefix() {
        every { accountRepository.findByExternalKey("0001000000") } returns null
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(any(), any())
        } returns emptyList()

        val response = service.getDailySalesHistories(allBranchScope, "0001000000", "202607")

        assertThat(response.sapAccountCode).isEqualTo("1000000")
        verify {
            dailySalesHistoryRepository
                .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc("1000000", "202607")
        }
    }

    @Test
    @DisplayName("매출월 형식이 잘못되면 INVALID_PARAMETER")
    fun rejectsInvalidSalesMonth() {
        assertThatThrownBy { service.getDailySalesHistories(allBranchScope, "1000000", "2026") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_PARAMETER")

        assertThatThrownBy { service.getDailySalesHistories(allBranchScope, "1000000", "202613") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_PARAMETER")
    }

    @Test
    @DisplayName("거래처가 없으면 ACCOUNT_NOT_FOUND")
    fun rejectsUnknownAccount() {
        every { accountRepository.findByExternalKey(any()) } returns null

        assertThatThrownBy { service.getDailySalesHistories(allBranchScope, "9999999", "202607") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_NOT_FOUND")
    }

    @Test
    @DisplayName("거래처 지점이 권한 범위 밖이면 AdminForbiddenException")
    fun rejectsOutOfScopeAccount() {
        every { accountRepository.findByExternalKey("1000000") } returns account(branchCode = "9900")

        val limitedScope = DataScope(branchCodes = listOf("1100"), isAllBranches = false)

        assertThatThrownBy { service.getDailySalesHistories(limitedScope, "1000000", "202607") }
            .isInstanceOf(AdminForbiddenException::class.java)
    }
}
