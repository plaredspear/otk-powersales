package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryBackfillCounts
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PPTHistoryMasterIdBackfillService 테스트")
class PPTHistoryMasterIdBackfillServiceTest {

    private val pptHistoryRepository: PPTHistoryRepository = mockk()
    private val service = PPTHistoryMasterIdBackfillService(pptHistoryRepository)

    private fun counts(backfillable: Long, ambiguous: Long): PPTHistoryBackfillCounts =
        object : PPTHistoryBackfillCounts {
            override val backfillable: Long = backfillable
            override val ambiguous: Long = ambiguous
        }

    @Test
    @DisplayName("preview: 단일 매칭 / 후보 다중 건수를 그대로 전달 (변경 없음)")
    fun preview_delegates() {
        every { pptHistoryRepository.countMasterIdBackfillTargets() } returns counts(8000L, 12L)

        val result = service.preview()

        assertThat(result.backfillable).isEqualTo(8000L)
        assertThat(result.ambiguous).isEqualTo(12L)
        verify(exactly = 0) { pptHistoryRepository.backfillMasterIds(any()) }
    }

    @Test
    @DisplayName("backfill: 갱신 건수 + 실행 후 잔여/모호 건수를 반환")
    fun backfill_returnsUpdatedAndRemaining() {
        every { pptHistoryRepository.backfillMasterIds(1000) } returns 1000
        every { pptHistoryRepository.countMasterIdBackfillTargets() } returns counts(7000L, 12L)

        val result = service.backfill(1000)

        assertThat(result.updated).isEqualTo(1000)
        assertThat(result.remaining).isEqualTo(7000L)
        assertThat(result.ambiguous).isEqualTo(12L)
    }

    @Test
    @DisplayName("backfill: 상한을 넘는 limit 는 MAX_LIMIT_PER_RUN 으로 clamp")
    fun backfill_clampsUpperBound() {
        every { pptHistoryRepository.backfillMasterIds(PPTHistoryMasterIdBackfillService.MAX_LIMIT_PER_RUN) } returns 5000
        every { pptHistoryRepository.countMasterIdBackfillTargets() } returns counts(0L, 0L)

        service.backfill(999_999)

        verify(exactly = 1) {
            pptHistoryRepository.backfillMasterIds(PPTHistoryMasterIdBackfillService.MAX_LIMIT_PER_RUN)
        }
    }

    @Test
    @DisplayName("backfill: 0 이하 limit 는 1 로 clamp")
    fun backfill_clampsLowerBound() {
        every { pptHistoryRepository.backfillMasterIds(1) } returns 1
        every { pptHistoryRepository.countMasterIdBackfillTargets() } returns counts(0L, 0L)

        service.backfill(0)

        verify(exactly = 1) { pptHistoryRepository.backfillMasterIds(1) }
    }

    @Test
    @DisplayName("backfill: 대상이 없으면 갱신 0 / 잔여 0 (멱등 재실행)")
    fun backfill_noTargets() {
        every { pptHistoryRepository.backfillMasterIds(1000) } returns 0
        every { pptHistoryRepository.countMasterIdBackfillTargets() } returns counts(0L, 3L)

        val result = service.backfill(1000)

        assertThat(result.updated).isZero()
        assertThat(result.remaining).isZero()
        // 후보 다중분은 재실행해도 줄지 않는다.
        assertThat(result.ambiguous).isEqualTo(3L)
    }
}
