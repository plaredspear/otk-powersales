package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sales.service.DailySalesHistoryUpsertService
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertFailedRow
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertResult
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.DailySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
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

@DisplayName("SapDailySalesHistoryService 어댑터 테스트")
class SapDailySalesHistoryServiceTest {

    private val dailySalesHistoryUpsertService: DailySalesHistoryUpsertService = mockk()

    private lateinit var service: SapDailySalesHistoryService

    @BeforeEach
    fun setUp() {
        // 단위 테스트에서는 Spring 프록시 없이 ChunkedUpsertHelper 를 직접 사용 (@Transactional 무효, action 직접 invoke)
        val helper = ChunkedUpsertHelper()
        service = SapDailySalesHistoryService(
            dailySalesHistoryUpsertService = dailySalesHistoryUpsertService,
            chunkedUpsertHelper = helper,
            chunkSize = 3,
            maxRows = 100
        )
    }

    private fun item(
        sapAccountCode: String? = "1032619",
        salesDate: String? = "20260427"
    ): DailySalesHistoryRequestItem = DailySalesHistoryRequestItem(
        sapAccountCode = sapAccountCode,
        salesDate = salesDate
    )

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 단일 청크, 도메인 결과 (success=1, failure=0) → SalesHistoryDetail + chunk SUCCESS")
        fun happy_singleChunkSuccess() {
            every { dailySalesHistoryUpsertService.upsert(any()) } returns
                DailySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            val detail = service.upsert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.chunks).hasSize(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_SUCCESS)
        }

        @Test
        @DisplayName("청크 분할: 5건 / chunkSize=3 → 2 chunks (3+2), 도메인 호출 2회")
        fun chunkSplit_callsDomainPerChunk() {
            val items = (1..5).map { item(salesDate = "2026042$it") }
            every { dailySalesHistoryUpsertService.upsert(any()) } answers {
                val cmds = firstArg<List<DailySalesHistoryUpsertCommand>>()
                DailySalesHistoryUpsertResult(successCount = cmds.size, failureCount = 0, failures = emptyList())
            }

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(5)
            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].count).isEqualTo(3)
            assertThat(detail.chunks[1].count).isEqualTo(2)
            assertThat(detail.chunks).allMatch { it.status == ChunkResult.STATUS_SUCCESS }
            verify(exactly = 2) { dailySalesHistoryUpsertService.upsert(any()) }
            assertThat(detail.chunkCount).isEqualTo(2)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑, chunk PARTIAL")
        fun partialFailure_chunkPartial() {
            every { dailySalesHistoryUpsertService.upsert(any()) } returns
                DailySalesHistoryUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(DailySalesHistoryUpsertFailedRow("10326192026-04-27", "SalesDate 형식 오류: 2026-04-27"))
                )

            val detail = service.upsert(listOf(item(salesDate = "2026-04-27"), item(salesDate = "20260428")))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_PARTIAL)
            assertThat(detail.failures.single().reason).contains("SalesDate 형식 오류")
        }

        @Test
        @DisplayName("청크 commit 실패: 첫 청크 throw → chunk FAILED, 둘째 청크는 SUCCESS")
        fun chunkCommitFailure_isolated() {
            val items = (1..5).map { item(salesDate = "2026042$it") }
            var callCount = 0
            every { dailySalesHistoryUpsertService.upsert(any()) } answers {
                callCount++
                if (callCount == 1) throw RuntimeException("DB connection lost")
                val cmds = firstArg<List<DailySalesHistoryUpsertCommand>>()
                DailySalesHistoryUpsertResult(successCount = cmds.size, failureCount = 0, failures = emptyList())
            }

            val detail = service.upsert(items)

            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].status).isEqualTo(ChunkResult.STATUS_FAILED)
            assertThat(detail.chunks[1].status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(3)
        }

        @Test
        @DisplayName("DTO 매핑: DailySalesHistoryRequestItem → DailySalesHistoryUpsertCommand 필드 매핑")
        fun dtoMapping_itemToCommand() {
            every { dailySalesHistoryUpsertService.upsert(any()) } returns
                DailySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            val items = listOf(
                DailySalesHistoryRequestItem(
                    sapAccountCode = "1032619",
                    salesDate = "20260427",
                    erpSalesAmount1 = "1500000",
                    ledgerAmount = "1500000"
                )
            )

            service.upsert(items)

            val captor = slot<List<DailySalesHistoryUpsertCommand>>()
            verify { dailySalesHistoryUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.sapAccountCode).isEqualTo("1032619")
            assertThat(command.salesDate).isEqualTo("20260427")
            assertThat(command.erpSalesAmount1).isEqualTo("1500000")
            assertThat(command.ledgerAmount).isEqualTo("1500000")
        }
    }

    @Nested
    @DisplayName("upsert - 어댑터 정책")
    inner class AdapterPolicies {

        @Test
        @DisplayName("행 수 한도 초과 - SapPayloadTooLargeException, 도메인 호출 없음")
        fun upsert_payloadTooLarge() {
            val items = (1..101).map { item(salesDate = "20260" + (100 + it)) }

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(exactly = 0) { dailySalesHistoryUpsertService.upsert(any()) }
        }
    }
}
