package com.otoki.powersales.domain.sales.materialize

import com.otoki.powersales.domain.sales.materialize.OroraDailyMaterializeResult
import com.otoki.powersales.domain.sales.materialize.OroraDailySalesMaterializeService
import com.otoki.powersales.domain.sales.materialize.OroraMonthlyMaterializeResult
import com.otoki.powersales.domain.sales.materialize.OroraMonthlySalesMaterializeService
import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * [OroraSalesMaterializeFacade] 대상 매출월 산출 검증.
 *
 * 월배치는 **전월**(익월 초 실행 → 전월 마감분 적재, 레거시 ORORA 월배치 동등),
 * 일배치는 **당월**을 service 에 전달해야 한다.
 */
@DisplayName("OroraSalesMaterializeFacade 대상 매출월 산출")
class OroraSalesMaterializeFacadeTest {

    private val monthlyService: OroraMonthlySalesMaterializeService = mockk()
    private val dailyService: OroraDailySalesMaterializeService = mockk()
    private val facade = OroraSalesMaterializeFacade(
        monthlyService = monthlyService,
        dailyService = dailyService,
        rangeFrom = 1000000,
        rangeTo = 1100000,
        chunkSize = 2000,
    )

    private val yyyymm = DateTimeFormatter.ofPattern("yyyyMM")

    @Test
    @DisplayName("월배치: salesMonth 미지정 시 전월(now-1month)을 service 에 전달")
    fun monthlyResolvesPreviousMonth() {
        val captured = slot<String>()
        every { monthlyService.materialize(capture(captured), any()) } returns
                OroraMonthlyMaterializeResult("ignored", 0, 0, 0)

        facade.materializeMonthly()

        val expectedPrevMonth = LocalDate.now().minusMonths(1).format(yyyymm)
        assertThat(captured.captured).isEqualTo(expectedPrevMonth)
        verify(exactly = 1) { monthlyService.materialize(any(), any()) }
    }

    @Test
    @DisplayName("월배치: salesMonth 명시 시 그 값을 그대로 전달")
    fun monthlyHonorsExplicitMonth() {
        val captured = slot<String>()
        every { monthlyService.materialize(capture(captured), any()) } returns
                OroraMonthlyMaterializeResult("ignored", 0, 0, 0)

        facade.materializeMonthly("202601")

        assertThat(captured.captured).isEqualTo("202601")
    }

    @Test
    @DisplayName("일배치: salesMonth 미지정 시 당월(now)을 service 에 전달")
    fun dailyResolvesCurrentMonth() {
        val captured = slot<String>()
        every { dailyService.materialize(capture(captured), any()) } returns
                OroraDailyMaterializeResult("ignored", 0, 0)

        facade.materializeDaily()

        val expectedCurrentMonth = LocalDate.now().format(yyyymm)
        assertThat(captured.captured).isEqualTo(expectedCurrentMonth)
    }

    @Test
    @DisplayName("monthlyChunkCount 는 전체 거래처 범위의 청크 개수를 반환")
    fun monthlyChunkCountReflectsAccountRange() {
        // 1000000~1100000 / 2000 = 51 chunk
        assertThat(facade.monthlyChunkCount()).isEqualTo(51)
    }

    @Test
    @DisplayName("월배치 chunk: 선택한 chunkIndex 의 단일 거래처 구간 range 만 service 에 전달")
    fun monthlyChunkPassesSingleChunkRange() {
        val month = slot<String>()
        val range = slot<OroraAccountRange>()
        every { monthlyService.materialize(capture(month), capture(range)) } returns
                OroraMonthlyMaterializeResult("ignored", 0, 0, 0)

        // index 1 = 두 번째 chunk (1002000~1003999)
        facade.materializeMonthlyChunk(chunkIndex = 1, salesMonth = "202601")

        assertThat(month.captured).isEqualTo("202601")
        assertThat(range.captured.toChunks()).containsExactly("0001002000" to "0001003999")
        verify(exactly = 1) { monthlyService.materialize(any(), any()) }
    }

    @Test
    @DisplayName("월배치 chunk: salesMonth 미지정 시 전월 산출 + 선택 chunk range")
    fun monthlyChunkResolvesPreviousMonth() {
        val month = slot<String>()
        every { monthlyService.materialize(capture(month), any()) } returns
                OroraMonthlyMaterializeResult("ignored", 0, 0, 0)

        facade.materializeMonthlyChunk(chunkIndex = 0)

        assertThat(month.captured).isEqualTo(LocalDate.now().minusMonths(1).format(yyyymm))
    }
}
