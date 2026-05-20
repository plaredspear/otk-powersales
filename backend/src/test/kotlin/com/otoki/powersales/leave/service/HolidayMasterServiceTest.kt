package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.enums.HolidayType
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("HolidayMasterService 테스트")
class HolidayMasterServiceTest {

    private val holidayMasterRepository: HolidayMasterRepository = mockk()

    private val holidayMasterService = HolidayMasterService(
        holidayMasterRepository,
    )

    @Nested
    @DisplayName("isHoliday - 공휴일 여부 확인")
    inner class IsHolidayTests {

        @Test
        @DisplayName("공휴일인 날짜 -> true")
        fun isHoliday_true() {
            every { holidayMasterRepository.existsByHolidayDate(LocalDate.of(2026, 1, 1)) } returns true

            val result = holidayMasterService.isHoliday(LocalDate.of(2026, 1, 1))

            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("평일인 날짜 -> false")
        fun isHoliday_false() {
            every { holidayMasterRepository.existsByHolidayDate(LocalDate.of(2026, 1, 2)) } returns false

            val result = holidayMasterService.isHoliday(LocalDate.of(2026, 1, 2))

            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("getHolidaysByDateRange - 기간별 공휴일 조회")
    inner class GetHolidaysByDateRangeTests {

        @Test
        @DisplayName("기간 내 공휴일 반환")
        fun getHolidaysByDateRange_success() {
            val holidays = listOf(
                HolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정", type = HolidayType.PUBLIC_HOLIDAY, year = 2026),
                HolidayMaster(id = 2, holidayDate = LocalDate.of(2026, 1, 28), name = "설날 연휴", type = HolidayType.PUBLIC_HOLIDAY, year = 2026)
            )
            every {
                holidayMasterRepository.findByHolidayDateBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
            } returns holidays

            val result = holidayMasterService.getHolidaysByDateRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)
            )

            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("기간 내 공휴일 없음 -> 빈 리스트")
        fun getHolidaysByDateRange_empty() {
            every {
                holidayMasterRepository.findByHolidayDateBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
            } returns emptyList()

            val result = holidayMasterService.getHolidaysByDateRange(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)
            )

            assertThat(result).isEmpty()
        }
    }
}
