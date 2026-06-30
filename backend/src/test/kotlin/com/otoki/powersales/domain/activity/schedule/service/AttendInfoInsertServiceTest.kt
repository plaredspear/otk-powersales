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
    @DisplayName("insert - 레거시 정합 (수신 필드 명시 필수/형식 검증 제거 — raw 적재)")
    inner class InsertLegacyAlignment {

        @Test
        @DisplayName("EmployeeCode 누락 - 검증 없이 raw 적재, employeeCode=null")
        fun insert_missingEmployeeCode_stored() {
            val savedSlot = slot<List<AttendInfo>>()
            every { attendInfoRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<AttendInfo>>() }

            val result = service.insert(listOf(command(employeeCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.single().employeeCode).isNull()
        }

        @Test
        @DisplayName("StartDate/EndDate 형식 오류 - 거부하지 않고 문자열 그대로 raw 적재 (레거시 Text 컬럼 정합)")
        fun insert_invalidDates_storedRaw() {
            val savedSlot = slot<List<AttendInfo>>()
            every { attendInfoRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<AttendInfo>>() }

            val result = service.insert(listOf(command(startDate = "2026-04-27", endDate = "bad")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            val saved = savedSlot.captured.single()
            assertThat(saved.startDate).isEqualTo("2026-04-27")
            assertThat(saved.endDate).isEqualTo("bad")
        }

        @Test
        @DisplayName("필수 4필드 모두 누락 - 거부 없이 raw 적재 (null 그대로)")
        fun insert_allMissing_stored() {
            val savedSlot = slot<List<AttendInfo>>()
            every { attendInfoRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<AttendInfo>>() }

            val result = service.insert(
                listOf(command(employeeCode = null, startDate = null, endDate = null, attendType = null))
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            val saved = savedSlot.captured.single()
            assertThat(saved.employeeCode).isNull()
            assertThat(saved.startDate).isNull()
            assertThat(saved.attendType).isNull()
        }

        @Test
        @DisplayName("여러 행 - 누락 행 포함 전부 적재 (검증 게이트 없음)")
        fun insert_multipleRows_allStored() {
            mockSaveAll()

            val result = service.insert(listOf(command(employeeCode = null), command()))

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
        }
    }
}
