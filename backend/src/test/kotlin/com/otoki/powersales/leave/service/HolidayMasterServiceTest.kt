package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("HolidayMasterService 테스트")
class HolidayMasterServiceTest {

    @Mock
    private lateinit var holidayMasterRepository: HolidayMasterRepository

    @InjectMocks
    private lateinit var holidayMasterService: HolidayMasterService

    @Nested
    @DisplayName("isHoliday - 공휴일 여부 확인")
    inner class IsHolidayTests {

        @Test
        @DisplayName("공휴일인 날짜 -> true")
        fun isHoliday_true() {
            whenever(holidayMasterRepository.existsByHolidayDate(LocalDate.of(2026, 1, 1))).thenReturn(true)

            val result = holidayMasterService.isHoliday(LocalDate.of(2026, 1, 1))

            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("평일인 날짜 -> false")
        fun isHoliday_false() {
            whenever(holidayMasterRepository.existsByHolidayDate(LocalDate.of(2026, 1, 2))).thenReturn(false)

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
                HolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정", type = "법정공휴일", year = 2026),
                HolidayMaster(id = 2, holidayDate = LocalDate.of(2026, 1, 28), name = "설날 연휴", type = "법정공휴일", year = 2026)
            )
            whenever(holidayMasterRepository.findByHolidayDateBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)
            )).thenReturn(holidays)

            val result = holidayMasterService.getHolidaysByDateRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)
            )

            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("기간 내 공휴일 없음 -> 빈 리스트")
        fun getHolidaysByDateRange_empty() {
            whenever(holidayMasterRepository.findByHolidayDateBetween(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)
            )).thenReturn(emptyList())

            val result = holidayMasterService.getHolidaysByDateRange(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)
            )

            assertThat(result).isEmpty()
        }
    }
}
