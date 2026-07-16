package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TeamMemberScheduleNameBackfillService 테스트")
class TeamMemberScheduleNameBackfillServiceTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator = mockk()
    private val service = TeamMemberScheduleNameBackfillService(
        teamMemberScheduleRepository,
        teamMemberScheduleNameGenerator,
    )

    @Test
    @DisplayName("countMissing: repository countMissingName 위임")
    fun countMissing_delegates() {
        every { teamMemberScheduleRepository.countMissingName() } returns 42L

        assertThat(service.countMissing()).isEqualTo(42L)
    }

    @Test
    @DisplayName("backfill: 대상 id 마다 채번해 name UPDATE + 결과 집계")
    fun backfill_updatesEachTarget() {
        every { teamMemberScheduleRepository.findMissingNameIds(1000) } returns listOf(1L, 2L, 3L)
        every { teamMemberScheduleNameGenerator.next() } returnsMany listOf("TS00000001", "TS00000002", "TS00000003")
        every { teamMemberScheduleRepository.updateNameById(any(), any()) } returns 1
        every { teamMemberScheduleRepository.countMissingName() } returns 0L

        val result = service.backfill(1000)

        assertThat(result.processed).isEqualTo(3)
        assertThat(result.updated).isEqualTo(3)
        assertThat(result.remaining).isEqualTo(0L)
        // 각 id 에 대응하는 채번값으로 UPDATE 되었는지 검증.
        verify(exactly = 1) { teamMemberScheduleRepository.updateNameById(1L, "TS00000001") }
        verify(exactly = 1) { teamMemberScheduleRepository.updateNameById(2L, "TS00000002") }
        verify(exactly = 1) { teamMemberScheduleRepository.updateNameById(3L, "TS00000003") }
        verify(exactly = 3) { teamMemberScheduleNameGenerator.next() }
    }

    @Test
    @DisplayName("backfill: UPDATE 0 반환분(그새 채워짐)은 updated 에서 제외")
    fun backfill_countsOnlyActualUpdates() {
        every { teamMemberScheduleRepository.findMissingNameIds(1000) } returns listOf(1L, 2L)
        every { teamMemberScheduleNameGenerator.next() } returnsMany listOf("TS00000001", "TS00000002")
        every { teamMemberScheduleRepository.updateNameById(1L, any()) } returns 1
        every { teamMemberScheduleRepository.updateNameById(2L, any()) } returns 0 // 동시성으로 그새 채워짐
        every { teamMemberScheduleRepository.countMissingName() } returns 0L

        val result = service.backfill(1000)

        assertThat(result.processed).isEqualTo(2)
        assertThat(result.updated).isEqualTo(1)
    }

    @Test
    @DisplayName("backfill: 잔여가 남으면 remaining 으로 재실행 필요를 표기")
    fun backfill_reportsRemaining() {
        every { teamMemberScheduleRepository.findMissingNameIds(2) } returns listOf(1L, 2L)
        every { teamMemberScheduleNameGenerator.next() } returnsMany listOf("TS00000001", "TS00000002")
        every { teamMemberScheduleRepository.updateNameById(any(), any()) } returns 1
        every { teamMemberScheduleRepository.countMissingName() } returns 5L

        val result = service.backfill(2)

        assertThat(result.updated).isEqualTo(2)
        assertThat(result.remaining).isEqualTo(5L)
    }

    @Test
    @DisplayName("backfill: limit 은 [1, MAX_LIMIT_PER_RUN] 로 clamp")
    fun backfill_clampsLimit() {
        every { teamMemberScheduleRepository.findMissingNameIds(any()) } returns emptyList()
        every { teamMemberScheduleRepository.countMissingName() } returns 0L

        service.backfill(999999)
        verify { teamMemberScheduleRepository.findMissingNameIds(TeamMemberScheduleNameBackfillService.MAX_LIMIT_PER_RUN) }

        service.backfill(0)
        verify { teamMemberScheduleRepository.findMissingNameIds(1) }
    }
}
