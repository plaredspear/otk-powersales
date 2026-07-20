package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TeamMemberScheduleNameGenerator 테스트")
class TeamMemberScheduleNameGeneratorTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val generator = TeamMemberScheduleNameGenerator(teamMemberScheduleRepository)

    @Test
    @DisplayName("next: 시퀀스값을 TS + 8자리 zero-padded 로 포맷 (SF AutoNumber TS{00000000} 재현)")
    fun next_formatsAsTsWithEightDigits() {
        every { teamMemberScheduleRepository.getNextNameSeq() } returns 1L

        val name = generator.next()

        assertThat(name).isEqualTo("TS00000001")
        verify(exactly = 1) { teamMemberScheduleRepository.getNextNameSeq() }
    }

    @Test
    @DisplayName("next: 큰 시퀀스값도 8자리 유지 (오버플로우 시 그대로 확장)")
    fun next_largeSeq() {
        every { teamMemberScheduleRepository.getNextNameSeq() } returns 12345L
        assertThat(generator.next()).isEqualTo("TS00012345")

        every { teamMemberScheduleRepository.getNextNameSeq() } returns 123456789L
        // 8자리를 넘으면 잘리지 않고 그대로 (String.format %08d 는 최소 폭만 보장)
        assertThat(generator.next()).isEqualTo("TS123456789")
    }

    @Test
    @DisplayName("next: 매 호출마다 채번 쿼리를 호출 (벌크 생성 시 건별 채번 정합)")
    fun next_callsSeqEachTime() {
        every { teamMemberScheduleRepository.getNextNameSeq() } returnsMany listOf(1L, 2L, 3L)

        val names = listOf(generator.next(), generator.next(), generator.next())

        assertThat(names).containsExactly("TS00000001", "TS00000002", "TS00000003")
        verify(exactly = 3) { teamMemberScheduleRepository.getNextNameSeq() }
    }

    @Test
    @DisplayName("nextBatch: 채번 쿼리 1회로 count 개를 순번 오름차순 발급 (구간 끝값 기준 역산)")
    fun nextBatch_allocatesBlockInSingleQuery() {
        // 구간 끝값 3 → 발급 구간 [1, 3]
        every { teamMemberScheduleRepository.allocateNameSeqBlock(3L) } returns 3L

        val names = generator.nextBatch(3)

        assertThat(names).containsExactly("TS00000001", "TS00000002", "TS00000003")
        verify(exactly = 1) { teamMemberScheduleRepository.allocateNameSeqBlock(3L) }
        verify(exactly = 0) { teamMemberScheduleRepository.getNextNameSeq() }
    }

    @Test
    @DisplayName("nextBatch: 기존 최대 번호 이후 구간을 이어서 발급")
    fun nextBatch_continuesAfterExistingMax() {
        every { teamMemberScheduleRepository.allocateNameSeqBlock(2L) } returns 12346L

        assertThat(generator.nextBatch(2)).containsExactly("TS00012345", "TS00012346")
    }

    @Test
    @DisplayName("nextBatch: count 가 0 이하면 채번하지 않고 빈 리스트")
    fun nextBatch_zeroOrNegative_doesNotAllocate() {
        assertThat(generator.nextBatch(0)).isEmpty()
        assertThat(generator.nextBatch(-1)).isEmpty()

        verify(exactly = 0) { teamMemberScheduleRepository.allocateNameSeqBlock(any()) }
    }
}
