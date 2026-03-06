package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapDailySalesRequest.ReqItem
import com.otoki.internal.sap.entity.DailySalesHistory
import com.otoki.internal.sap.repository.DailySalesHistoryRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapDailySalesService 테스트")
class SapDailySalesServiceTest {

    @Mock
    private lateinit var dailySalesHistoryRepository: DailySalesHistoryRepository

    @InjectMocks
    private lateinit var sapDailySalesService: SapDailySalesService

    @Nested
    @DisplayName("sync - 신규 일별매출 등록")
    inner class NewDailySalesTests {

        @Test
        @DisplayName("정상 등록 - DB에 없는 externalKey -> Insert")
        fun sync_newDailySales_creates() {
            val items = listOf(createReqItem(
                sapAccountCode = "ACC001",
                salesDate = "20240101",
                erpSalesAmount1 = "1000.5",
                erpSalesAmount2 = "2000",
                erpSalesAmount3 = "3000",
                erpDistributionAmount1 = "100",
                erpDistributionAmount2 = "200",
                erpDistributionAmount3 = "300",
                ledgerAmount = "5000"
            ))
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(null)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            val result = sapDailySalesService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<DailySalesHistory>()
            verify(dailySalesHistoryRepository).save(captor.capture())
            assertThat(captor.firstValue.sapAccountCode).isEqualTo("ACC001")
            assertThat(captor.firstValue.salesDate).isEqualTo("20240101")
            assertThat(captor.firstValue.erpSalesAmount1).isEqualTo(1000.5)
            assertThat(captor.firstValue.erpSalesAmount2).isEqualTo(2000.0)
            assertThat(captor.firstValue.erpSalesAmount3).isEqualTo(3000.0)
            assertThat(captor.firstValue.erpDistributionAmount1).isEqualTo(100.0)
            assertThat(captor.firstValue.erpDistributionAmount2).isEqualTo(200.0)
            assertThat(captor.firstValue.erpDistributionAmount3).isEqualTo(300.0)
            assertThat(captor.firstValue.ledgerAmount).isEqualTo(5000.0)
        }
    }

    @Nested
    @DisplayName("sync - 기존 일별매출 업데이트")
    inner class ExistingDailySalesTests {

        @Test
        @DisplayName("기존 업데이트 - 필드 변경 및 updatedAt 설정")
        fun sync_existingDailySales_updates() {
            val existing = DailySalesHistory(
                id = 1,
                sapAccountCode = "ACC001",
                salesDate = "20240101",
                externalKey = "ACC00120240101"
            )
            existing.erpSalesAmount1 = 500.0
            val items = listOf(createReqItem(
                sapAccountCode = "ACC001",
                salesDate = "20240101",
                erpSalesAmount1 = "9999",
                ledgerAmount = "7777"
            ))
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(existing)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            val result = sapDailySalesService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.erpSalesAmount1).isEqualTo(9999.0)
            assertThat(existing.ledgerAmount).isEqualTo(7777.0)
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 숫자 변환")
    inner class NumberConversionTests {

        @Test
        @DisplayName("null 처리 - ledgerAmount=null -> 0.0")
        fun sync_nullNumber_defaultsToZero() {
            val items = listOf(createReqItem(sapAccountCode = "ACC001", salesDate = "20240101", ledgerAmount = null))
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(null)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            sapDailySalesService.sync(items)

            val captor = argumentCaptor<DailySalesHistory>()
            verify(dailySalesHistoryRepository).save(captor.capture())
            assertThat(captor.firstValue.ledgerAmount).isEqualTo(0.0)
        }

        @Test
        @DisplayName("빈 문자열 처리 - erpSalesAmount1=\"\" -> 0.0")
        fun sync_emptyNumber_defaultsToZero() {
            val items = listOf(createReqItem(sapAccountCode = "ACC001", salesDate = "20240101", erpSalesAmount1 = ""))
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(null)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            sapDailySalesService.sync(items)

            val captor = argumentCaptor<DailySalesHistory>()
            verify(dailySalesHistoryRepository).save(captor.capture())
            assertThat(captor.firstValue.erpSalesAmount1).isEqualTo(0.0)
        }

        @Test
        @DisplayName("잘못된 숫자 - erpSalesAmount1=\"abc\" -> 해당 레코드 실패")
        fun sync_invalidNumber_fails() {
            val items = listOf(createReqItem(sapAccountCode = "ACC001", salesDate = "20240101", erpSalesAmount1 = "abc"))
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(null)

            val result = sapDailySalesService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("sap_account_code 누락 - 해당 레코드 실패")
        fun sync_missingSapAccountCode_fails() {
            val items = listOf(createReqItem(sapAccountCode = null, salesDate = "20240101"))

            val result = sapDailySalesService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("sap_account_code")
        }

        @Test
        @DisplayName("sales_date 누락 - 해당 레코드 실패")
        fun sync_missingSalesDate_fails() {
            val items = listOf(createReqItem(sapAccountCode = "ACC001", salesDate = null))

            val result = sapDailySalesService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("sales_date")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(sapAccountCode = "ACC001", salesDate = "20240101"),
                createReqItem(sapAccountCode = null, salesDate = "20240102"),
                createReqItem(sapAccountCode = "ACC003", salesDate = "20240103")
            )
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00120240101")).thenReturn(null)
            whenever(dailySalesHistoryRepository.findByExternalKey("ACC00320240103")).thenReturn(null)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            val result = sapDailySalesService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("sync - external_key 생성")
    inner class ExternalKeyTests {

        @Test
        @DisplayName("external_key = sapAccountCode + salesDate 결합")
        fun sync_externalKey_combination() {
            val items = listOf(createReqItem(sapAccountCode = "VENDOR99", salesDate = "20241231"))
            whenever(dailySalesHistoryRepository.findByExternalKey("VENDOR9920241231")).thenReturn(null)
            whenever(dailySalesHistoryRepository.save(any<DailySalesHistory>()))
                .thenAnswer { it.getArgument<DailySalesHistory>(0) }

            sapDailySalesService.sync(items)

            val captor = argumentCaptor<DailySalesHistory>()
            verify(dailySalesHistoryRepository).save(captor.capture())
            assertThat(captor.firstValue.externalKey).isEqualTo("VENDOR9920241231")
        }
    }

    private fun createReqItem(
        sapAccountCode: String? = null,
        salesDate: String? = null,
        erpSalesAmount1: String? = null,
        erpSalesAmount2: String? = null,
        erpSalesAmount3: String? = null,
        erpDistributionAmount1: String? = null,
        erpDistributionAmount2: String? = null,
        erpDistributionAmount3: String? = null,
        ledgerAmount: String? = null
    ) = ReqItem(
        sapAccountCode = sapAccountCode,
        salesDate = salesDate,
        erpSalesAmount1 = erpSalesAmount1,
        erpSalesAmount2 = erpSalesAmount2,
        erpSalesAmount3 = erpSalesAmount3,
        erpDistributionAmount1 = erpDistributionAmount1,
        erpDistributionAmount2 = erpDistributionAmount2,
        erpDistributionAmount3 = erpDistributionAmount3,
        ledgerAmount = ledgerAmount
    )
}
