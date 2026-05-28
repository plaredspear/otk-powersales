package com.otoki.powersales.sales.service

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.orora.repository.OroraMonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.transaction.CannotCreateTransactionException
import java.math.BigDecimal

/**
 * [OroraMonthlySalesHistoryQueryGateway] 의 graceful fallback 동작 검증.
 *
 * - 정상 응답 통과
 * - 예외 catch → emptyList 반환
 * - 빈 입력 → repository 호출 skip
 */
@DisplayName("OroraMonthlySalesHistoryQueryGateway 동작 검증")
class OroraMonthlySalesHistoryQueryGatewayTest {

    private val repository: OroraMonthlySalesHistoryRepository = mockk()
    private val gateway = OroraMonthlySalesHistoryQueryGateway(repository)

    private fun row(sap: String, salesDate: String) = OroraMonthlySalesHistory(
        sapAccountCode = sap,
        salesDate = salesDate,
        abcClosingAmount1 = BigDecimal.ONE,
    )

    @Test
    @DisplayName("findBySalesDate: 정상 응답 통과")
    fun findBySalesDateOk() {
        every { repository.findBySalesDateAndSapAccountCodeIn("202605", listOf("SAP1")) } returns
            listOf(row("SAP1", "202605"))

        val result = gateway.findBySalesDate("202605", listOf("SAP1"))

        assertThat(result).hasSize(1)
        assertThat(result.first().sapAccountCode).isEqualTo("SAP1")
    }

    @Test
    @DisplayName("findBySalesDate: CannotCreateTransactionException → emptyList fallback")
    fun findBySalesDateCannotCreateTransactionFallback() {
        every { repository.findBySalesDateAndSapAccountCodeIn(any(), any()) } throws
            CannotCreateTransactionException("VPN 미도달")

        val result = gateway.findBySalesDate("202605", listOf("SAP1"))

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findBySalesDate: DataAccessException → emptyList fallback")
    fun findBySalesDateDataAccessFallback() {
        every { repository.findBySalesDateAndSapAccountCodeIn(any(), any()) } throws
            DataAccessResourceFailureException("connection refused")

        val result = gateway.findBySalesDate("202605", listOf("SAP1"))

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findBySalesDate: 빈 sapAccountCodes → repository 호출 skip")
    fun findBySalesDateEmptyInputSkip() {
        val result = gateway.findBySalesDate("202605", emptyList())

        assertThat(result).isEmpty()
        verify(exactly = 0) { repository.findBySalesDateAndSapAccountCodeIn(any(), any()) }
    }

    @Test
    @DisplayName("findBySalesDates: 정상 응답 통과")
    fun findBySalesDatesOk() {
        every {
            repository.findBySalesDateInAndSapAccountCodeIn(listOf("202605", "202604"), listOf("SAP1"))
        } returns listOf(row("SAP1", "202605"), row("SAP1", "202604"))

        val result = gateway.findBySalesDates(listOf("202605", "202604"), listOf("SAP1"))

        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("findBySalesDates: 예외 → emptyList fallback")
    fun findBySalesDatesFallback() {
        every { repository.findBySalesDateInAndSapAccountCodeIn(any(), any()) } throws
            RuntimeException("ORORA timeout")

        val result = gateway.findBySalesDates(listOf("202605"), listOf("SAP1"))

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findBySalesDates: 빈 sapAccountCodes 또는 빈 salesDates → repository 호출 skip")
    fun findBySalesDatesEmptyInputSkip() {
        val r1 = gateway.findBySalesDates(listOf("202605"), emptyList())
        val r2 = gateway.findBySalesDates(emptyList(), listOf("SAP1"))

        assertThat(r1).isEmpty()
        assertThat(r2).isEmpty()
        verify(exactly = 0) { repository.findBySalesDateInAndSapAccountCodeIn(any(), any()) }
    }
}
