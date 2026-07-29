package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("AdminMonthlySalesHistoryService 테스트")
class AdminMonthlySalesHistoryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk()
    private val service = AdminMonthlySalesHistoryService(accountRepository, monthlySalesHistoryRepository)

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

    private fun monthly(
        abcSum: Double? = null,
        shipSum: Double? = null,
        abc1: Double? = null,
        ship1: Double? = null,
        isDeleted: Boolean? = null,
        updatedAt: LocalDateTime = LocalDateTime.of(2026, 8, 9, 11, 30),
    ): MonthlySalesHistory = MonthlySalesHistory(
        sapAccountCode = "1000000",
        salesYear = SalesYear.Y2026,
        salesMonth = SalesMonth.M07,
        externalkeyC = "1000000202607",
        isDeleted = isDeleted,
    ).also {
        it.abcClosingSumAmount = abcSum
        it.shipClosingSumAmount = shipSum
        it.abcClosingAmount1 = abc1
        it.shipClosingAmount1 = ship1
        it.updatedAt = updatedAt
    }

    @Test
    @DisplayName("거래처코드 + 매출년월로 월매출 행을 조회하고 합계 컬럼을 합산한다")
    fun returnsRowsWithTotals() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            monthlySalesHistoryRepository.findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(
                "1000000",
                SalesYear.Y2026,
                SalesMonth.M07,
            )
        } returns listOf(monthly(abcSum = 12000.0, shipSum = 3400.0, abc1 = 5000.0, ship1 = 1000.0))

        val response = service.getMonthlySalesHistories(allBranchScope, "1000000", "202607")

        assertThat(response.salesMonth).isEqualTo("202607")
        assertThat(response.sapAccountCode).isEqualTo("1000000")
        assertThat(response.accountName).isEqualTo("GS25 역삼점")
        assertThat(response.branchName).isEqualTo("강남지점")
        assertThat(response.content).hasSize(1)
        assertThat(response.content[0].salesYear).isEqualTo("2026")
        assertThat(response.content[0].salesMonth).isEqualTo("07")
        assertThat(response.content[0].abcClosingAmount1).isEqualTo(5000.0)
        assertThat(response.content[0].shipClosingAmount1).isEqualTo(1000.0)
        assertThat(response.totalAbcClosingAmount).isEqualTo(12000.0)
        assertThat(response.totalShipClosingAmount).isEqualTo(3400.0)
    }

    @Test
    @DisplayName("합계는 적재된 합계 컬럼 기준 — 온도대별 1~4 를 재합산하지 않는다")
    fun sumsClosingSumColumnsNotPerTemperature() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        // 개별 온도대 컬럼이 비고 합계 컬럼에만 값이 든 행 (SF 이관분에 실제 존재하는 형태).
        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns listOf(monthly(abcSum = 9000.0, shipSum = 800.0, abc1 = null, ship1 = null))

        val response = service.getMonthlySalesHistories(allBranchScope, "1000000", "202607")

        assertThat(response.totalAbcClosingAmount).isEqualTo(9000.0)
        assertThat(response.totalShipClosingAmount).isEqualTo(800.0)
    }

    @Test
    @DisplayName("soft-delete 행은 목록에는 남기되 합계에서는 제외한다")
    fun excludesSoftDeletedRowsFromTotalsButKeepsThemInContent() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns listOf(
            monthly(abcSum = 10000.0, shipSum = 2000.0),
            monthly(abcSum = 7777.0, shipSum = 999.0, isDeleted = true),
        )

        val response = service.getMonthlySalesHistories(allBranchScope, "1000000", "202607")

        // 목록은 전 행 노출 (적재 결과 확인이 목적인 화면).
        assertThat(response.content).hasSize(2)
        assertThat(response.content.map { it.isDeleted }).containsExactly(false, true)
        // 합계는 삭제 행 제외 — 기존 read 경로(MonthlySalesHistoryQueryGateway) 와 금액이 갈리지 않게.
        assertThat(response.totalAbcClosingAmount).isEqualTo(10000.0)
        assertThat(response.totalShipClosingAmount).isEqualTo(2000.0)
    }

    @Test
    @DisplayName("마지막 적재 시각은 조회한 거래처+년월 행의 max(updatedAt), 결과 0건이면 null")
    fun returnsLastMaterializedAtOfQueriedMonth() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns listOf(
            monthly(updatedAt = LocalDateTime.of(2026, 8, 9, 11, 30)),
            monthly(updatedAt = LocalDateTime.of(2026, 8, 9, 11, 35)),
        )

        assertThat(service.getMonthlySalesHistories(allBranchScope, "1000000", "202607").lastMaterializedAt)
            .isEqualTo(LocalDateTime.of(2026, 8, 9, 11, 35))

        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns emptyList()

        assertThat(service.getMonthlySalesHistories(allBranchScope, "1000000", "202605").lastMaterializedAt)
            .isNull()
    }

    @Test
    @DisplayName("매출년월에 하이픈이 섞여 있어도 yyyyMM 으로 정규화해 조회한다")
    fun normalizesHyphenatedSalesMonth() {
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns emptyList()

        val response = service.getMonthlySalesHistories(allBranchScope, "1000000", "2026-07")

        assertThat(response.salesMonth).isEqualTo("202607")
        verify {
            monthlySalesHistoryRepository.findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(
                "1000000",
                SalesYear.Y2026,
                SalesMonth.M07,
            )
        }
    }

    @Test
    @DisplayName("ORORA 원본 형식(선행 000) 거래처코드는 prefix 를 떼고 재조회한다")
    fun stripsOroraAccountCodePrefix() {
        every { accountRepository.findByExternalKey("0001000000") } returns null
        every { accountRepository.findByExternalKey("1000000") } returns account()
        every {
            monthlySalesHistoryRepository
                .findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(any(), any(), any())
        } returns emptyList()

        val response = service.getMonthlySalesHistories(allBranchScope, "0001000000", "202607")

        assertThat(response.sapAccountCode).isEqualTo("1000000")
        verify {
            monthlySalesHistoryRepository.findBySapAccountCodeAndSalesYearAndSalesMonthOrderByIdAsc(
                "1000000",
                SalesYear.Y2026,
                SalesMonth.M07,
            )
        }
    }

    @Test
    @DisplayName("매출년월 형식이 잘못되거나 적재 지원 연도 밖이면 INVALID_PARAMETER")
    fun rejectsInvalidSalesMonth() {
        assertThatThrownBy { service.getMonthlySalesHistories(allBranchScope, "1000000", "2026") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_PARAMETER")

        assertThatThrownBy { service.getMonthlySalesHistories(allBranchScope, "1000000", "202613") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_PARAMETER")

        // sales_year 는 SF picklist enum(2019~2030) 컬럼이라 범위 밖 연도는 적재 자체가 불가능하다.
        assertThatThrownBy { service.getMonthlySalesHistories(allBranchScope, "1000000", "201812") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "INVALID_PARAMETER")
    }

    @Test
    @DisplayName("거래처가 없으면 ACCOUNT_NOT_FOUND")
    fun rejectsUnknownAccount() {
        every { accountRepository.findByExternalKey(any()) } returns null

        assertThatThrownBy { service.getMonthlySalesHistories(allBranchScope, "9999999", "202607") }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_NOT_FOUND")
    }

    @Test
    @DisplayName("거래처 지점이 권한 범위 밖이면 AdminForbiddenException")
    fun rejectsOutOfScopeAccount() {
        every { accountRepository.findByExternalKey("1000000") } returns account(branchCode = "9900")

        val limitedScope = DataScope(branchCodes = listOf("1100"), isAllBranches = false)

        assertThatThrownBy { service.getMonthlySalesHistories(limitedScope, "1000000", "202607") }
            .isInstanceOf(AdminForbiddenException::class.java)
    }
}
