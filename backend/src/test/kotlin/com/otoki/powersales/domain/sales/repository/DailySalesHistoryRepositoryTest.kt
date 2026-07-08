package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("DailySalesHistoryRepository 테스트")
class DailySalesHistoryRepositoryTest {

    @Autowired
    private lateinit var dailySalesHistoryRepository: DailySalesHistoryRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        dailySalesHistoryRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persist(
        sapCode: String,
        salesDate: String,
        erpSales: Double?,
        erpDist: Double?,
        ledger: Double? = null,
    ): DailySalesHistory = testEntityManager.persist(
        DailySalesHistory(
            sapAccountCode = sapCode,
            salesDate = salesDate,
            externalKey = sapCode + salesDate,
            erpSalesAmount = erpSales,
            erpDistributionAmount = erpDist,
            ledgerAmount = ledger,
        ),
    )

    @Test
    @DisplayName("sumMonthlyBySapAccountCodeIn - 거래처별로 해당 월 일별 금액을 SUM 집계한다")
    fun sumsPerAccountForTargetMonth() {
        persist("1000077", "20260115", 1000.0, 200.0, ledger = 30.0)
        persist("1000077", "20260116", 500.0, 100.0)
        persist("1000088", "20260120", 700.0, null)

        val rows = dailySalesHistoryRepository
            .sumMonthlyBySapAccountCodeIn(listOf("1000077", "1000088"), "202601")
            .associateBy { it.sapAccountCode }

        assertThat(rows).hasSize(2)
        assertThat(rows["1000077"]!!.erpSalesSum).isEqualTo(1500.0)
        assertThat(rows["1000077"]!!.erpDistributionSum).isEqualTo(300.0)
        assertThat(rows["1000077"]!!.ledgerSum).isEqualTo(30.0) // null 은 SUM 에서 무시
        assertThat(rows["1000088"]!!.erpSalesSum).isEqualTo(700.0)
        assertThat(rows["1000088"]!!.erpDistributionSum).isNull() // 전부 null → SUM null
        assertThat(rows["1000088"]!!.ledgerSum).isNull()
    }

    @Test
    @DisplayName("sumMonthlyBySapAccountCodeIn - 대상 월 밖 row 와 목록 밖 거래처는 집계에서 제외한다")
    fun excludesOtherMonthsAndAccounts() {
        persist("1000077", "20260115", 1000.0, 200.0)
        persist("1000077", "20251231", 9999.0, 9999.0) // 전월 — 제외
        persist("1000077", "20260201", 8888.0, 8888.0) // 익월 — 제외
        persist("1000099", "20260115", 7777.0, 7777.0) // 목록 밖 거래처 — 제외

        val rows = dailySalesHistoryRepository.sumMonthlyBySapAccountCodeIn(listOf("1000077"), "202601")

        assertThat(rows).hasSize(1)
        assertThat(rows.first().sapAccountCode).isEqualTo("1000077")
        assertThat(rows.first().erpSalesSum).isEqualTo(1000.0)
        assertThat(rows.first().erpDistributionSum).isEqualTo(200.0)
    }

    @Test
    @DisplayName("sumMonthlyBySapAccountCodeIn - 해당 월 row 가 없는 거래처는 결과에 나타나지 않는다")
    fun accountWithoutRowsIsAbsent() {
        persist("1000077", "20260115", 1000.0, 200.0)

        val rows = dailySalesHistoryRepository.sumMonthlyBySapAccountCodeIn(listOf("1000077", "1000088"), "202601")

        assertThat(rows).hasSize(1)
        assertThat(rows.first().sapAccountCode).isEqualTo("1000077")
    }

    @Test
    @DisplayName("sumMonthlyBySapAccountCodeBetween - 코드 범위 안 거래처만 월 SUM 집계한다")
    fun sumsByCodeRangeForTargetMonth() {
        persist("1000077", "20260115", 1000.0, 200.0, ledger = 30.0)
        persist("1000077", "20260116", 500.0, 100.0)
        persist("1000200", "20260120", 700.0, 50.0) // 범위 안
        persist("1002500", "20260120", 9999.0, 9999.0) // 범위 밖 (상한 초과)
        persist("1000077", "20251231", 8888.0, 8888.0) // 전월 — 제외

        val rows = dailySalesHistoryRepository
            .sumMonthlyBySapAccountCodeBetween("1000000", "1001999", "202601")
            .associateBy { it.sapAccountCode }

        assertThat(rows.keys).containsExactlyInAnyOrder("1000077", "1000200")
        assertThat(rows["1000077"]!!.erpSalesSum).isEqualTo(1500.0)
        assertThat(rows["1000077"]!!.erpDistributionSum).isEqualTo(300.0)
        assertThat(rows["1000077"]!!.ledgerSum).isEqualTo(30.0)
        assertThat(rows["1000200"]!!.erpSalesSum).isEqualTo(700.0)
    }
}
