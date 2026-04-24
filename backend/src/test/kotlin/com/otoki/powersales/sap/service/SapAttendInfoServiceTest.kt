package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.dto.SapAttendInfoRequest.ReqItem
import com.otoki.powersales.sap.entity.AttendInfo
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.AttendInfoRepository
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAttendInfoService 테스트")
class SapAttendInfoServiceTest {

    @Mock
    private lateinit var attendInfoRepository: AttendInfoRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var sapAttendInfoService: SapAttendInfoService

    @Nested
    @DisplayName("sync - 출퇴근 등록")
    inner class SyncTests {

        @Test
        @DisplayName("정상 등록 - 모든 필드 매핑")
        fun sync_success_allFieldsMapped() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val items = listOf(createReqItem(
                employeeCode = "100234",
                startDate = "20260301",
                endDate = "20260301",
                attendType = "연차",
                status = "승인"
            ))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<AttendInfo>()
            verify(attendInfoRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.employeeCode).isEqualTo("100234")
            assertThat(saved.startDate).isEqualTo("20260301")
            assertThat(saved.endDate).isEqualTo("20260301")
            assertThat(saved.attendType).isEqualTo("연차")
            assertThat(saved.status).isEqualTo("승인")
        }

        @Test
        @DisplayName("중복 호출 - 동일 데이터 2회 -> 2건 Insert")
        fun sync_duplicateCall_insertsAll() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val item = createReqItem(employeeCode = "100234", startDate = "20260301")
            val result = sapAttendInfoService.sync(listOf(item, item))

            assertThat(result.successCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("employee_code 누락 - 해당 레코드 실패")
        fun sync_missingEmployeeCode_fails() {
            val items = listOf(createReqItem(employeeCode = null, startDate = "20260301"))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("employee_code")
        }

        @Test
        @DisplayName("start_date 누락 - 해당 레코드 실패")
        fun sync_missingStartDate_fails() {
            val items = listOf(createReqItem(employeeCode = "100234", startDate = null))
            val result = sapAttendInfoService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("start_date")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val items = listOf(
                createReqItem(employeeCode = "100001", startDate = "20260301"),
                createReqItem(employeeCode = null, startDate = "20260302"),
                createReqItem(employeeCode = "100003", startDate = "20260303")
            )
            val result = sapAttendInfoService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("sync - 청크 처리")
    inner class ChunkTests {

        @Test
        @DisplayName("CHUNK_SIZE 확인 - 5000")
        fun chunkSize_is5000() {
            assertThat(SapAttendInfoService.CHUNK_SIZE).isEqualTo(5_000)
        }
    }

    @Nested
    @DisplayName("연차 스케줄 생성")
    inner class AnnualLeaveScheduleCreationTests {

        @Test
        @DisplayName("연차(14) - startDate=20260305, endDate=20260307 -> 3건 TeamMemberSchedule 생성")
        fun sync_annualLeave_createsSchedulesForDateRange() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }
            whenever(employeeRepository.findByEmployeeCode("100234"))
                .thenReturn(Optional.of(createEmployee(id = 100L, employeeCode = "100234")))
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(false)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>()))
                .thenAnswer { it.getArgument<TeamMemberSchedule>(0) }

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260305",
                endDate = "20260307",
                attendType = "14"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)

            val captor = argumentCaptor<TeamMemberSchedule>()
            verify(teamMemberScheduleRepository, times(3)).save(captor.capture())

            val savedSchedules = captor.allValues
            assertThat(savedSchedules).hasSize(3)
            assertThat(savedSchedules.map { it.workingDate }).containsExactly(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 7)
            )
            savedSchedules.forEach { schedule ->
                assertThat(schedule.employee?.id).isEqualTo(100L)
                assertThat(schedule.workingType).isEqualTo("연차")
            }
        }

        @Test
        @DisplayName("경조(90) - startDate=20260310, endDate=20260310 -> 1건 스케줄 생성")
        fun sync_familyEvent_createsOneSchedule() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }
            whenever(employeeRepository.findByEmployeeCode("100500"))
                .thenReturn(Optional.of(createEmployee(id = 200L, employeeCode = "100500")))
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(false)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>()))
                .thenAnswer { it.getArgument<TeamMemberSchedule>(0) }

            val item = createReqItem(
                employeeCode = "100500",
                startDate = "20260310",
                endDate = "20260310",
                attendType = "90"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)

            val captor = argumentCaptor<TeamMemberSchedule>()
            verify(teamMemberScheduleRepository, times(1)).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.employee?.id).isEqualTo(200L)
            assertThat(saved.workingDate).isEqualTo(LocalDate.of(2026, 3, 10))
            assertThat(saved.workingType).isEqualTo("연차")
        }

        @Test
        @DisplayName("endDate null - attendType=14, startDate=20260315 -> startDate 1건만 생성")
        fun sync_endDateNull_createsOneScheduleForStartDate() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }
            whenever(employeeRepository.findByEmployeeCode("100234"))
                .thenReturn(Optional.of(createEmployee(id = 100L, employeeCode = "100234")))
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(false)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>()))
                .thenAnswer { it.getArgument<TeamMemberSchedule>(0) }

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260315",
                endDate = null,
                attendType = "14"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)

            val captor = argumentCaptor<TeamMemberSchedule>()
            verify(teamMemberScheduleRepository, times(1)).save(captor.capture())

            val saved = captor.firstValue
            assertThat(saved.employee?.id).isEqualTo(100L)
            assertThat(saved.workingDate).isEqualTo(LocalDate.of(2026, 3, 15))
            assertThat(saved.workingType).isEqualTo("연차")
        }
    }

    @Nested
    @DisplayName("연차 스케줄 삭제 (취소)")
    inner class AnnualLeaveScheduleCancellationTests {

        @Test
        @DisplayName("status=Y - 연차 취소 -> deleteAnnualLeaveByEmployeeIdAndDateRange 호출")
        fun sync_statusY_deletesAnnualLeaveSchedules() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }
            whenever(employeeRepository.findByEmployeeCode("100234"))
                .thenReturn(Optional.of(createEmployee(id = 100L, employeeCode = "100234")))
            whenever(teamMemberScheduleRepository.deleteAnnualLeaveByEmployeeIdAndDateRange(
                any(), any(), any()
            )).thenReturn(3L)

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260305",
                endDate = "20260307",
                attendType = "14",
                status = "Y"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)

            verify(teamMemberScheduleRepository).deleteAnnualLeaveByEmployeeIdAndDateRange(
                eq(100L),
                eq(LocalDate.of(2026, 3, 5)),
                eq(LocalDate.of(2026, 3, 7))
            )
            verify(teamMemberScheduleRepository, never()).save(any<TeamMemberSchedule>())
        }
    }

    @Nested
    @DisplayName("연차 스케줄 중복 방지")
    inner class AnnualLeaveDuplicatePreventionTests {

        @Test
        @DisplayName("이미 존재하는 스케줄 - existsBy 반환 true -> save 미호출")
        fun sync_duplicateScheduleExists_skipsSave() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }
            whenever(employeeRepository.findByEmployeeCode("100234"))
                .thenReturn(Optional.of(createEmployee(id = 100L, employeeCode = "100234")))
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(true)

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260305",
                endDate = "20260305",
                attendType = "14"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)

            verify(teamMemberScheduleRepository).existsByEmployeeAndWorkingDateAndWorkingType(
                any(),
                eq(LocalDate.of(2026, 3, 5)),
                eq("연차")
            )
            verify(teamMemberScheduleRepository, never()).save(any<TeamMemberSchedule>())
        }
    }

    @Nested
    @DisplayName("연차 스케줄 비대상")
    inner class AnnualLeaveNonTargetTests {

        @Test
        @DisplayName("비대상 근태코드(30) - 스케줄 생성/삭제 미호출")
        fun sync_nonTargetAttendType_noScheduleInteraction() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260305",
                endDate = "20260305",
                attendType = "30"
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)

            verify(teamMemberScheduleRepository, never()).save(any<TeamMemberSchedule>())
            verify(teamMemberScheduleRepository, never()).existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )
            verify(teamMemberScheduleRepository, never()).deleteAnnualLeaveByEmployeeIdAndDateRange(
                any(), any(), any()
            )
        }

        @Test
        @DisplayName("attendType null - 스케줄 생성/삭제 미호출")
        fun sync_nullAttendType_noScheduleInteraction() {
            // Given
            whenever(attendInfoRepository.save(any<AttendInfo>()))
                .thenAnswer { it.getArgument<AttendInfo>(0) }

            val item = createReqItem(
                employeeCode = "100234",
                startDate = "20260305",
                endDate = "20260305",
                attendType = null
            )

            // When
            val result = sapAttendInfoService.sync(listOf(item))

            // Then
            assertThat(result.successCount).isEqualTo(1)

            verify(teamMemberScheduleRepository, never()).save(any<TeamMemberSchedule>())
            verify(teamMemberScheduleRepository, never()).existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )
            verify(teamMemberScheduleRepository, never()).deleteAnnualLeaveByEmployeeIdAndDateRange(
                any(), any(), any()
            )
        }
    }

    private fun createEmployee(
        id: Long = 100L,
        employeeCode: String = "100234"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "테스트사원"
    )

    private fun createReqItem(
        employeeCode: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        attendType: String? = null,
        status: String? = null
    ) = ReqItem(
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )
}
