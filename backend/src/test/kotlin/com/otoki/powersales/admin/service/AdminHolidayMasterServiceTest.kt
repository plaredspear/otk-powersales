package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.HolidayMasterCreateRequest
import com.otoki.powersales.admin.dto.request.HolidayMasterUpdateRequest
import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.exception.HolidayDateDuplicateException
import com.otoki.powersales.leave.exception.HolidayNotFoundException
import com.otoki.powersales.leave.exception.InvalidHolidayTypeException
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import com.otoki.powersales.leave.service.AdminHolidayMasterService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminHolidayMasterService 테스트")
class AdminHolidayMasterServiceTest {

    @Mock
    private lateinit var holidayMasterRepository: HolidayMasterRepository

    @InjectMocks
    private lateinit var adminHolidayMasterService: AdminHolidayMasterService

    @Nested
    @DisplayName("getHolidayMasters - 연도별 공휴일 목록 조회")
    inner class GetHolidayMastersTests {

        @Test
        @DisplayName("정상 조회 - 2026년 공휴일 목록 반환")
        fun getHolidayMasters_success() {
            val holidays = listOf(
                createHolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정"),
                createHolidayMaster(id = 2, holidayDate = LocalDate.of(2026, 3, 1), name = "삼일절")
            )
            whenever(holidayMasterRepository.findByYearOrderByHolidayDateAsc(2026)).thenReturn(holidays)

            val result = adminHolidayMasterService.getHolidayMasters(2026)

            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("신정")
            assertThat(result[1].name).isEqualTo("삼일절")
        }

        @Test
        @DisplayName("빈 결과 - 등록된 공휴일 없는 연도 -> 빈 리스트")
        fun getHolidayMasters_empty() {
            whenever(holidayMasterRepository.findByYearOrderByHolidayDateAsc(2099)).thenReturn(emptyList())

            val result = adminHolidayMasterService.getHolidayMasters(2099)

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("createHolidayMaster - 공휴일 등록")
    inner class CreateHolidayMasterTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> 공휴일 생성 반환")
        fun createHolidayMaster_success() {
            val request = HolidayMasterCreateRequest(
                holidayDate = LocalDate.of(2026, 8, 17),
                name = "임시공휴일",
                type = "임시공휴일"
            )
            whenever(holidayMasterRepository.existsByHolidayDate(request.holidayDate)).thenReturn(false)
            whenever(holidayMasterRepository.save(any<HolidayMaster>())).thenAnswer { it.getArgument<HolidayMaster>(0) }

            val result = adminHolidayMasterService.createHolidayMaster(request)

            assertThat(result.name).isEqualTo("임시공휴일")
            assertThat(result.type).isEqualTo("임시공휴일")
            assertThat(result.holidayDate).isEqualTo(LocalDate.of(2026, 8, 17))
        }

        @Test
        @DisplayName("중복 날짜 - 이미 존재하는 날짜 -> HolidayDateDuplicateException")
        fun createHolidayMaster_duplicateDate() {
            val request = HolidayMasterCreateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정",
                type = "법정공휴일"
            )
            whenever(holidayMasterRepository.existsByHolidayDate(request.holidayDate)).thenReturn(true)

            assertThatThrownBy { adminHolidayMasterService.createHolidayMaster(request) }
                .isInstanceOf(HolidayDateDuplicateException::class.java)
        }

        @Test
        @DisplayName("잘못된 유형 - 허용되지 않는 type -> InvalidHolidayTypeException")
        fun createHolidayMaster_invalidType() {
            val request = HolidayMasterCreateRequest(
                holidayDate = LocalDate.of(2026, 8, 17),
                name = "기타",
                type = "기타"
            )

            assertThatThrownBy { adminHolidayMasterService.createHolidayMaster(request) }
                .isInstanceOf(InvalidHolidayTypeException::class.java)
        }
    }

    @Nested
    @DisplayName("updateHolidayMaster - 공휴일 수정")
    inner class UpdateHolidayMasterTests {

        @Test
        @DisplayName("정상 수정 - 이름 변경 -> 수정된 공휴일 반환")
        fun updateHolidayMaster_success() {
            val existing = createHolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정")
            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정(수정)",
                type = "법정공휴일"
            )
            whenever(holidayMasterRepository.findById(1L)).thenReturn(Optional.of(existing))
            whenever(holidayMasterRepository.existsByHolidayDateAndIdNot(request.holidayDate, 1L)).thenReturn(false)

            val result = adminHolidayMasterService.updateHolidayMaster(1L, request)

            assertThat(result.name).isEqualTo("신정(수정)")
        }

        @Test
        @DisplayName("존재하지 않는 ID - id=99999 -> HolidayNotFoundException")
        fun updateHolidayMaster_notFound() {
            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정",
                type = "법정공휴일"
            )
            whenever(holidayMasterRepository.findById(99999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminHolidayMasterService.updateHolidayMaster(99999L, request) }
                .isInstanceOf(HolidayNotFoundException::class.java)
        }

        @Test
        @DisplayName("중복 날짜 - 다른 공휴일과 날짜 충돌 -> HolidayDateDuplicateException")
        fun updateHolidayMaster_duplicateDate() {
            val existing = createHolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정")
            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 3, 1),
                name = "신정",
                type = "법정공휴일"
            )
            whenever(holidayMasterRepository.findById(1L)).thenReturn(Optional.of(existing))
            whenever(holidayMasterRepository.existsByHolidayDateAndIdNot(request.holidayDate, 1L)).thenReturn(true)

            assertThatThrownBy { adminHolidayMasterService.updateHolidayMaster(1L, request) }
                .isInstanceOf(HolidayDateDuplicateException::class.java)
        }

        @Test
        @DisplayName("잘못된 유형 - 허용되지 않는 type -> InvalidHolidayTypeException")
        fun updateHolidayMaster_invalidType() {
            val existing = createHolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정")
            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정",
                type = "기타"
            )
            whenever(holidayMasterRepository.findById(1L)).thenReturn(Optional.of(existing))

            assertThatThrownBy { adminHolidayMasterService.updateHolidayMaster(1L, request) }
                .isInstanceOf(InvalidHolidayTypeException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteHolidayMaster - 공휴일 삭제")
    inner class DeleteHolidayMasterTests {

        @Test
        @DisplayName("정상 삭제 - 존재하는 공휴일 삭제")
        fun deleteHolidayMaster_success() {
            val existing = createHolidayMaster(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정")
            whenever(holidayMasterRepository.findById(1L)).thenReturn(Optional.of(existing))

            adminHolidayMasterService.deleteHolidayMaster(1L)
        }

        @Test
        @DisplayName("존재하지 않는 ID - id=99999 -> HolidayNotFoundException")
        fun deleteHolidayMaster_notFound() {
            whenever(holidayMasterRepository.findById(99999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminHolidayMasterService.deleteHolidayMaster(99999L) }
                .isInstanceOf(HolidayNotFoundException::class.java)
        }
    }

    private fun createHolidayMaster(
        id: Long = 1L,
        holidayDate: LocalDate = LocalDate.of(2026, 1, 1),
        name: String = "신정",
        type: String = "법정공휴일"
    ): HolidayMaster = HolidayMaster(
        id = id,
        holidayDate = holidayDate,
        name = name,
        type = type,
        year = holidayDate.year
    )
}
