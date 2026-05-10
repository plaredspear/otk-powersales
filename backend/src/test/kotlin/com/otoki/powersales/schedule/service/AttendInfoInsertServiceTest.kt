package com.otoki.powersales.schedule.service

import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.repository.AttendInfoRepository
import com.otoki.powersales.schedule.service.dto.AttendInfoInsertCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("AttendInfoInsertService 테스트")
class AttendInfoInsertServiceTest {

    @Mock
    private lateinit var attendInfoRepository: AttendInfoRepository

    @InjectMocks
    private lateinit var service: AttendInfoInsertService

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
        whenever(attendInfoRepository.saveAll(any<List<AttendInfo>>()))
            .thenAnswer { it.getArgument<List<AttendInfo>>(0) }
    }

    @Nested
    @DisplayName("insert - Happy Path")
    inner class InsertHappy {

        @Test
        @DisplayName("정상 1건 - INSERT, success_count=1")
        fun insert_success() {
            mockSaveAll()

            val result = service.insert(listOf(command()))

            val captor = argumentCaptor<List<AttendInfo>>()
            verify(attendInfoRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
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
            mockSaveAll()

            val result = service.insert(listOf(command(attendType = "ZZ_UNKNOWN")))

            val captor = argumentCaptor<List<AttendInfo>>()
            verify(attendInfoRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().attendType).isEqualTo("ZZ_UNKNOWN")
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
            verify(attendInfoRepository, never()).saveAll(any<List<AttendInfo>>())
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
