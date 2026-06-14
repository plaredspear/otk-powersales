package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.service.AttendInfoInsertService
import com.otoki.powersales.domain.activity.schedule.repository.AttendInfoRepository
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AttendInfoInsertService 테스트")
class AttendInfoInsertServiceTest {

    private val attendInfoRepository: AttendInfoRepository = mockk(relaxUnitFun = true)
    private val service = AttendInfoInsertService(attendInfoRepository)

    private fun command(
        employeeCode: String? = "100123",
        startDate: String? = "20260427",
        endDate: String? = "20260427",
        attendType: String? = "14",
        status: String? = "정상"
    ): AttendInfoInsertCommand = AttendInfoInsertCommand(
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )

    private fun mockSaveAll() {
        every { attendInfoRepository.saveAll(any<List<AttendInfo>>()) } answers { firstArg<List<AttendInfo>>() }
    }

    @Nested
    @DisplayName("insert - Happy Path")
    inner class InsertHappy {

        @Test
        @DisplayName("정상 1건 - INSERT, success_count=1")
        fun insert_success() {
            mockSaveAll()
            val savedSlot = slot<List<AttendInfo>>()
            every { attendInfoRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<AttendInfo>>() }

            val result = service.insert(listOf(command()))

            verify { attendInfoRepository.saveAll(any<List<AttendInfo>>()) }
            val saved = savedSlot.captured.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.startDate).isEqualTo("20260427")
            assertThat(saved.endDate).isEqualTo("20260427")
            assertThat(saved.attendType).isEqualTo("14")
            assertThat(saved.status).isEqualTo("정상")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.savedAttendInfos).hasSize(1)
        }

        @Test
        @DisplayName("AttendType 미매칭 - 원본 코드 그대로 저장 (D3 결정), 거부 안 함")
        fun insert_unknownAttendType() {
            val savedSlot = slot<List<AttendInfo>>()
            every { attendInfoRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<AttendInfo>>() }

            val result = service.insert(listOf(command(attendType = "ZZ_UNKNOWN")))

            verify { attendInfoRepository.saveAll(any<List<AttendInfo>>()) }
            assertThat(savedSlot.captured.single().attendType).isEqualTo("ZZ_UNKNOWN")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("insert - Error Path")
    inner class InsertError {

        @Test
        @DisplayName("EmployeeCode 누락 - failures, 적재 스킵")
        fun insert_missingEmployeeCode() {
            val result = service.insert(listOf(command(employeeCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("EmployeeCode 필수")
            verify(exactly = 0) { attendInfoRepository.saveAll(any<List<AttendInfo>>()) }
        }

        @Test
        @DisplayName("StartDate 누락 - failures")
        fun insert_missingStartDate() {
            val result = service.insert(listOf(command(startDate = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("StartDate 필수")
        }

        @Test
        @DisplayName("EndDate 누락 - failures")
        fun insert_missingEndDate() {
            val result = service.insert(listOf(command(endDate = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("EndDate 필수")
        }

        @Test
        @DisplayName("StartDate 형식 오류 - failures")
        fun insert_invalidStartDate() {
            val result = service.insert(listOf(command(startDate = "2026-04-27")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("StartDate YYYYMMDD 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun insert_partialFailure() {
            mockSaveAll()

            val result = service.insert(listOf(command(employeeCode = null), command()))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
        }
    }
}
