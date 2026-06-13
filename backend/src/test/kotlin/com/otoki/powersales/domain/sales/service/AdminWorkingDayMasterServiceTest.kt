package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import com.otoki.powersales.domain.sales.repository.WorkingDayMasterRepository
import com.otoki.powersales.domain.sales.service.AdminWorkingDayMasterService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AdminWorkingDayMasterService 테스트")
class AdminWorkingDayMasterServiceTest {

    private val workingDayMasterRepository: WorkingDayMasterRepository = mockk()
    private val service = AdminWorkingDayMasterService(workingDayMasterRepository)

    private fun day(date: String, check: Double?): WorkingDayMaster =
        WorkingDayMaster(workingDate = LocalDate.parse(date), workingDateCheck = check)

    @Test
    @DisplayName("연-월 조회 시 해당 월의 1일~말일 구간으로 repository 를 호출한다")
    fun queriesFullMonthRange() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every {
            workingDayMasterRepository.findByWorkingDateRange(capture(startSlot), capture(endSlot))
        } returns emptyList()

        service.getWorkingDayMasters(2026, 2)

        // 2월은 윤년 아닌 2026 기준 28일까지.
        assertThat(startSlot.captured).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(endSlot.captured).isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @Test
    @DisplayName("workingDateCheck = 1 인 행만 영업일로 집계하고 나머지는 휴일로 집계한다")
    fun countsWorkingDaysAndHolidays() {
        every { workingDayMasterRepository.findByWorkingDateRange(any(), any()) } returns listOf(
            day("2026-06-01", 1.0),
            day("2026-06-02", 1.0),
            day("2026-06-06", 0.0), // 공휴일
            day("2026-06-07", 0.0), // 일요일
            day("2026-06-08", null), // 값 미설정 → 휴일 취급
        )

        val response = service.getWorkingDayMasters(2026, 6)

        assertThat(response.content).hasSize(5)
        assertThat(response.workingDayCount).isEqualTo(2)
        assertThat(response.holidayCount).isEqualTo(3)
    }

    @Test
    @DisplayName("isWorkingDay 파생 값이 workingDateCheck == 1.0 여부로 산출된다")
    fun derivesIsWorkingDayFlag() {
        every { workingDayMasterRepository.findByWorkingDateRange(any(), any()) } returns listOf(
            day("2026-06-01", 1.0),
            day("2026-06-06", 0.0),
        )

        val response = service.getWorkingDayMasters(2026, 6)

        assertThat(response.content[0].isWorkingDay).isTrue()
        assertThat(response.content[1].isWorkingDay).isFalse()
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록과 0 집계를 반환한다")
    fun returnsEmptyWhenNoRows() {
        every { workingDayMasterRepository.findByWorkingDateRange(any(), any()) } returns emptyList()

        val response = service.getWorkingDayMasters(2026, 6)

        assertThat(response.content).isEmpty()
        assertThat(response.workingDayCount).isZero()
        assertThat(response.holidayCount).isZero()
    }

    @Test
    @DisplayName("month 가 1~12 범위를 벗어나면 IllegalArgumentException 을 던진다")
    fun rejectsOutOfRangeMonth() {
        Assertions.assertThatThrownBy { service.getWorkingDayMasters(2026, 13) }
            .isInstanceOf(IllegalArgumentException::class.java)
        Assertions.assertThatThrownBy { service.getWorkingDayMasters(2026, 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("(start, end) 오버로드는 전달받은 구간 그대로 repository 를 호출한다")
    fun startEndOverloadDelegatesRange() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every {
            workingDayMasterRepository.findByWorkingDateRange(capture(startSlot), capture(endSlot))
        } returns listOf(day("2026-06-10", 1.0))

        val response = service.getWorkingDayMasters(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 20))

        assertThat(startSlot.captured).isEqualTo(LocalDate.of(2026, 6, 5))
        assertThat(endSlot.captured).isEqualTo(LocalDate.of(2026, 6, 20))
        assertThat(response.content).hasSize(1)
        assertThat(response.workingDayCount).isEqualTo(1)
    }
}
