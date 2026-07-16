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
}
