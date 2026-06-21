package com.otoki.powersales.domain.sales.materialize

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.orora.repository.OroraMonthlySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.materialize.OroraMonthlySalesChunkProcessor
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * [OroraMonthlySalesChunkProcessor] 매핑/upsert 검증 (Spec #855).
 */
@DisplayName("OroraMonthlySalesChunkProcessor")
class OroraMonthlySalesChunkProcessorTest {

    private val ororaRepo: OroraMonthlySalesHistoryRepository = mockk()
    private val monthlyRepo: MonthlySalesHistoryRepository = mockk()
    private val accountRepo: AccountRepository = mockk()
    private val processor = OroraMonthlySalesChunkProcessor(ororaRepo, monthlyRepo, accountRepo)

    private fun ororaRow(sap: String) = OroraMonthlySalesHistory(
        sapAccountCode = sap,
        salesDate = "202605",
        abcClosingAmount1 = BigDecimal("100"),
        abcClosingAmount2 = BigDecimal("200"),
        shipClosingAmount1 = BigDecimal("10"),
    )

    @Test
    @DisplayName("ORORA row → monthly upsert: 선행 000 제거 + 마감 합계 산출 + external_key 구성")
    fun mapsAndUpserts() {
        every {
            ororaRepo.findBySalesDateAndSapAccountCodeBetween("202605", "0001000000", "0001001999")
        } returns listOf(ororaRow("0001000077"))
        every { accountRepo.findByExternalKeyIn(listOf("1000077")) } returns
            listOf(Account(id = 5, externalKey = "1000077", sfid = "001ACC"))
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(
                listOf(SalesYear.Y2026), listOf(SalesMonth.M05), listOf("1000077")
            )
        } returns emptyList()
        val saved = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(saved)) } answers { saved.captured }

        val result = processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        assertThat(result.fetched).isEqualTo(1)
        assertThat(result.upserted).isEqualTo(1)
        assertThat(result.unmatched).isEqualTo(0)

        val entity = saved.captured.first()
        assertThat(entity.sapAccountCode).isEqualTo("1000077")
        assertThat(entity.externalkeyC).isEqualTo("1000077" + "2026" + "05")
        assertThat(entity.abcClosingAmount1).isEqualTo(100.0)
        assertThat(entity.abcClosingSumAmount).isEqualTo(300.0) // 100 + 200 (3,4 null=0)
        assertThat(entity.shipClosingSumAmount).isEqualTo(10.0)
        assertThat(entity.account?.id).isEqualTo(5L)
        assertThat(entity.accountSfid).isEqualTo("001ACC")
    }

    @Test
    @DisplayName("account 미매칭 시 account_id=null 로 적재하고 unmatched 카운트")
    fun unmatchedAccount() {
        every {
            ororaRepo.findBySalesDateAndSapAccountCodeBetween("202605", "0001000000", "0001001999")
        } returns listOf(ororaRow("0001000099"))
        every { accountRepo.findByExternalKeyIn(listOf("1000099")) } returns emptyList()
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        val saved = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(saved)) } answers { saved.captured }

        val result = processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        assertThat(result.unmatched).isEqualTo(1)
        assertThat(saved.captured.first().account).isNull()
    }

    @Test
    @DisplayName("레거시 raw 정합: 개별 마감 1~4 컬럼은 source null → 0.0 으로 적재")
    fun nullClosingColumnsStoredAsZero() {
        // abc3/abc4, ship2/ship3/ship4 가 null 인 row → 레거시 doPost 처럼 0 으로 적재되어야 한다.
        every {
            ororaRepo.findBySalesDateAndSapAccountCodeBetween("202605", "0001000000", "0001001999")
        } returns listOf(ororaRow("0001000077"))
        every { accountRepo.findByExternalKeyIn(listOf("1000077")) } returns
            listOf(Account(id = 5, externalKey = "1000077", sfid = "001ACC"))
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        val saved = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(saved)) } answers { saved.captured }

        processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        val entity = saved.captured.first()
        // source null → null 유지(과거 동작)가 아니라 0.0 적재(레거시 raw)
        assertThat(entity.abcClosingAmount3).isEqualTo(0.0)
        assertThat(entity.abcClosingAmount4).isEqualTo(0.0)
        assertThat(entity.shipClosingAmount2).isEqualTo(0.0)
        assertThat(entity.shipClosingAmount3).isEqualTo(0.0)
        assertThat(entity.shipClosingAmount4).isEqualTo(0.0)
        // 값 있는 컬럼은 그대로
        assertThat(entity.abcClosingAmount1).isEqualTo(100.0)
        assertThat(entity.shipClosingAmount1).isEqualTo(10.0)
    }

    @Test
    @DisplayName("ORORA 조회 결과 비면 upsert 호출 없이 0 반환")
    fun emptyOrora() {
        every {
            ororaRepo.findBySalesDateAndSapAccountCodeBetween(any(), any(), any())
        } returns emptyList()

        val result = processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        assertThat(result.fetched).isEqualTo(0)
        assertThat(result.upserted).isEqualTo(0)
    }
}
