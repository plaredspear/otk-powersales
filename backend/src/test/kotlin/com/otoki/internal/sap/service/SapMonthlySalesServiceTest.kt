package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.MonthlySalesHistory
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.sap.dto.SapMonthlySalesRequest
import com.otoki.internal.sap.dto.SapSyncResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SapMonthlySalesServiceTest {

    @Mock
    lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @Mock
    lateinit var accountRepository: AccountRepository

    @InjectMocks
    lateinit var service: SapMonthlySalesService

    private fun createReqItem(
        sapAccountCode: String? = "ACC001",
        salesYearMonth: String? = "202603",
        abcClosingAmount1: String? = "100.0",
        abcClosingAmount2: String? = "200.0",
        abcClosingAmount3: String? = "300.0",
        totalLedgerAmount: String? = "999.0",
        shipClosingAmount: String? = "400.0",
        rlsales: String? = "500.0",
    ) = SapMonthlySalesRequest.ReqItem(
        sapAccountCode = sapAccountCode,
        salesYearMonth = salesYearMonth,
        abcClosingAmount1 = abcClosingAmount1,
        abcClosingAmount2 = abcClosingAmount2,
        abcClosingAmount3 = abcClosingAmount3,
        totalLedgerAmount = totalLedgerAmount,
        shipClosingAmount = shipClosingAmount,
        rlsales = rlsales,
    )

    @Nested
    @DisplayName("신규 Insert")
    inner class NewInsert {

        @Test
        @DisplayName("새로운 externalKey -> Insert, salesYear/salesMonth 분리 확인")
        fun `새로운 externalKey이면 Insert하고 salesYear salesMonth를 분리한다`() {
            // given
            val item = createReqItem(sapAccountCode = "ACC001", salesYearMonth = "202603")
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)

            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.accountExternalKey).isEqualTo("ACC001")
            assertThat(saved.salesYear).isEqualTo("2026")
            assertThat(saved.salesMonth).isEqualTo("03")
            assertThat(saved.externalkeyC).isEqualTo("ACC001202603")
            assertThat(saved.abcClosingAmount1).isEqualTo(100.0)
            assertThat(saved.abcClosingAmount2).isEqualTo(200.0)
            assertThat(saved.abcClosingAmount3).isEqualTo(300.0)
            assertThat(saved.shipClosingAmount).isEqualTo(400.0)
            assertThat(saved.rlsalesC).isEqualTo(500.0)
        }
    }

    @Nested
    @DisplayName("기존 Update")
    inner class ExistingUpdate {

        @Test
        @DisplayName("기존 데이터가 존재하면 Update한다")
        fun `기존 데이터가 존재하면 필드를 업데이트한다`() {
            // given
            val existing = MonthlySalesHistory(
                id = 1L,
                accountExternalKey = "ACC001",
                salesYear = "2026",
                salesMonth = "03",
                externalkeyC = "ACC001202603",
                abcClosingAmount1 = 50.0,
                abcClosingAmount2 = 60.0,
                abcClosingAmount3 = 70.0,
                shipClosingAmount = 80.0,
                rlsalesC = 90.0,
            )
            val item = createReqItem()
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(existing)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)

            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val updated = captor.firstValue
            assertThat(updated.id).isEqualTo(1L)
            assertThat(updated.abcClosingAmount1).isEqualTo(100.0)
            assertThat(updated.abcClosingAmount2).isEqualTo(200.0)
            assertThat(updated.abcClosingAmount3).isEqualTo(300.0)
            assertThat(updated.shipClosingAmount).isEqualTo(400.0)
            assertThat(updated.rlsalesC).isEqualTo(500.0)
        }
    }

    @Nested
    @DisplayName("salesYearMonth 분리")
    inner class SalesYearMonthSplit {

        @Test
        @DisplayName("202603 -> salesYear=2026, salesMonth=03")
        fun `salesYearMonth를 salesYear와 salesMonth로 분리한다`() {
            // given
            val item = createReqItem(salesYearMonth = "202603")
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            service.sync(listOf(item))

            // then
            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.salesYear).isEqualTo("2026")
            assertThat(saved.salesMonth).isEqualTo("03")
        }
    }

    @Nested
    @DisplayName("금액 파싱")
    inner class AmountParsing {

        @Test
        @DisplayName("금액이 null이면 0.0으로 변환")
        fun `금액이 null이면 0점0으로 변환한다`() {
            // given
            val item = createReqItem(
                abcClosingAmount1 = null,
                abcClosingAmount2 = null,
                abcClosingAmount3 = null,
                shipClosingAmount = null,
                rlsales = null,
            )
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(1)

            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.abcClosingAmount1).isEqualTo(0.0)
            assertThat(saved.abcClosingAmount2).isEqualTo(0.0)
            assertThat(saved.abcClosingAmount3).isEqualTo(0.0)
            assertThat(saved.shipClosingAmount).isEqualTo(0.0)
            assertThat(saved.rlsalesC).isEqualTo(0.0)
        }

        @Test
        @DisplayName("금액이 빈 문자열이면 0.0으로 변환")
        fun `금액이 빈 문자열이면 0점0으로 변환한다`() {
            // given
            val item = createReqItem(
                abcClosingAmount1 = "",
                abcClosingAmount2 = "  ",
                abcClosingAmount3 = "",
                shipClosingAmount = "",
                rlsales = "",
            )
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(1)

            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.abcClosingAmount1).isEqualTo(0.0)
            assertThat(saved.abcClosingAmount2).isEqualTo(0.0)
            assertThat(saved.abcClosingAmount3).isEqualTo(0.0)
            assertThat(saved.shipClosingAmount).isEqualTo(0.0)
            assertThat(saved.rlsalesC).isEqualTo(0.0)
        }

        @Test
        @DisplayName("잘못된 금액 문자열이면 실패 처리")
        fun `잘못된 금액 문자열이면 실패 처리한다`() {
            // given
            val item = createReqItem(abcClosingAmount1 = "abc")
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].index).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("필수 필드 누락")
    inner class RequiredFieldMissing {

        @Test
        @DisplayName("sap_account_code 누락 시 실패")
        fun `sapAccountCode가 null이면 실패한다`() {
            // given
            val item = createReqItem(sapAccountCode = null)

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].field).isEqualTo("sap_account_code")
            verify(monthlySalesHistoryRepository, never()).save(any<MonthlySalesHistory>())
        }

        @Test
        @DisplayName("sales_year_month 누락 시 실패")
        fun `salesYearMonth가 null이면 실패한다`() {
            // given
            val item = createReqItem(salesYearMonth = null)

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].field).isEqualTo("sap_account_code")
            verify(monthlySalesHistoryRepository, never()).save(any<MonthlySalesHistory>())
        }
    }

    @Nested
    @DisplayName("부분 실패")
    inner class PartialFailure {

        @Test
        @DisplayName("3건 중 1건 에러 시 부분 성공")
        fun `3건 중 1건이 에러이면 2건 성공 1건 실패`() {
            // given
            val items = listOf(
                createReqItem(sapAccountCode = "ACC001", salesYearMonth = "202601"),
                createReqItem(sapAccountCode = null, salesYearMonth = "202602"), // 실패
                createReqItem(sapAccountCode = "ACC003", salesYearMonth = "202603"),
            )
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202601")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC003202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(items)

            // then
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("total_ledger_amount 무시")
    inner class TotalLedgerAmountIgnored {

        @Test
        @DisplayName("total_ledger_amount는 엔티티에 매핑되지 않는다")
        fun `totalLedgerAmount는 엔티티 필드에 매핑되지 않는다`() {
            // given
            val item = createReqItem(totalLedgerAmount = "999999.99")
            whenever(monthlySalesHistoryRepository.findByExternalkeyC("ACC001202603")).thenReturn(null)
            whenever(monthlySalesHistoryRepository.save(any<MonthlySalesHistory>())).thenAnswer { it.arguments[0] }

            // when
            val result = service.sync(listOf(item))

            // then
            assertThat(result.successCount).isEqualTo(1)

            val captor = argumentCaptor<MonthlySalesHistory>()
            verify(monthlySalesHistoryRepository).save(captor.capture())

            val saved = captor.firstValue
            // total_ledger_amount(999999.99)는 어떤 필드에도 매핑되지 않아야 함
            assertThat(saved.abcClosingAmount1).isEqualTo(100.0)
            assertThat(saved.abcClosingAmount2).isEqualTo(200.0)
            assertThat(saved.abcClosingAmount3).isEqualTo(300.0)
            assertThat(saved.shipClosingAmount).isEqualTo(400.0)
            assertThat(saved.rlsalesC).isEqualTo(500.0)
        }
    }
}
