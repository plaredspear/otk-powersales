package com.otoki.powersales.sf.inbound.service

import com.otoki.powersales.sales.service.MonthlySalesHistoryUpsertService
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertFailedRow
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertResult
import com.otoki.powersales.sf.inbound.dto.sales.SfChunkResult
import com.otoki.powersales.sf.inbound.dto.sales.SfMonthlySalesHistoryRequestItem
import com.otoki.powersales.sf.inbound.exception.SfPayloadTooLargeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * REQUEST_ACCEPTED audit 검증은 SfInboundAuditAspect 가 책임 (별도 테스트).
 * 본 테스트는 어댑터의 도메인 호출 / DTO 매핑 / 청크 분할 / 응답 매핑만 검증.
 */
@DisplayName("SfMonthlySalesHistoryService 어댑터 테스트")
class SfMonthlySalesHistoryServiceTest {

    private val monthlySalesHistoryUpsertService: MonthlySalesHistoryUpsertService = mockk()

    private lateinit var service: SfMonthlySalesHistoryService

    @BeforeEach
    fun setUp() {
        val helper = SfChunkedUpsertHelper()
        service = SfMonthlySalesHistoryService(
            monthlySalesHistoryUpsertService = monthlySalesHistoryUpsertService,
            chunkedUpsertHelper = helper,
            chunkSize = 1000,
            maxRows = 50
        )
    }

    private fun item(
        sapAccountCode: String? = "1032619",
        salesYearMonth: String? = "202604"
    ): SfMonthlySalesHistoryRequestItem = SfMonthlySalesHistoryRequestItem(
        sapAccountCode = sapAccountCode,
        salesYearMonth = salesYearMonth
    )

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 단일 청크, 도메인 결과 → SfSalesHistoryDetail + chunk SUCCESS")
        fun happy_singleChunkSuccess() {
            every { monthlySalesHistoryUpsertService.upsert(any()) } returns
                MonthlySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            val detail = service.upsert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(SfChunkResult.STATUS_SUCCESS)
            assertThat(detail.chunkCount).isEqualTo(1)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SF SfFailureItem 매핑, chunk FAILED")
        fun partialFailure_chunkFailed() {
            every { monthlySalesHistoryUpsertService.upsert(any()) } returns
                MonthlySalesHistoryUpsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(MonthlySalesHistoryUpsertFailedRow("10326192026/04", "SalesYearMonth 형식 오류: 2026/04"))
                )

            val detail = service.upsert(listOf(item(salesYearMonth = "2026/04")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(SfChunkResult.STATUS_FAILED)
        }

        @Test
        @DisplayName("청크 commit 실패: throw → chunk FAILED")
        fun chunkCommitFailure() {
            every { monthlySalesHistoryUpsertService.upsert(any()) } throws RuntimeException("DB connection lost")

            val detail = service.upsert(listOf(item()))

            assertThat(detail.chunks.single().status).isEqualTo(SfChunkResult.STATUS_FAILED)
            assertThat(detail.failureCount).isEqualTo(1)
        }

        @Test
        @DisplayName("DTO 매핑: SfMonthlySalesHistoryRequestItem → MonthlySalesHistoryUpsertCommand")
        fun dtoMapping_itemToCommand() {
            val commandsSlot = slot<List<MonthlySalesHistoryUpsertCommand>>()
            every { monthlySalesHistoryUpsertService.upsert(capture(commandsSlot)) } returns
                MonthlySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            val items = listOf(
                SfMonthlySalesHistoryRequestItem(
                    sapAccountCode = "1032619",
                    salesYearMonth = "202604",
                    abcClosingAmount1 = "5000000",
                    totalLedgerAmount = "1000000",
                    shipClosingAmount = "4800000"
                )
            )

            service.upsert(items)

            val command = commandsSlot.captured.single()
            assertThat(command.sapAccountCode).isEqualTo("1032619")
            assertThat(command.salesYearMonth).isEqualTo("202604")
            assertThat(command.abcClosingAmount1).isEqualTo("5000000")
            assertThat(command.totalLedgerAmount).isEqualTo("1000000")
            assertThat(command.shipClosingAmount).isEqualTo("4800000")
        }
    }

    @Nested
    @DisplayName("upsert - 어댑터 정책")
    inner class AdapterPolicies {

        @Test
        @DisplayName("행 수 한도 초과 - SfPayloadTooLargeException, 도메인 호출 없음")
        fun upsert_payloadTooLarge() {
            val items = (1..51).map { idx -> item(sapAccountCode = "ACCT-$idx") }

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(SfPayloadTooLargeException::class.java)
            verify(exactly = 0) { monthlySalesHistoryUpsertService.upsert(any()) }
        }
    }
}
