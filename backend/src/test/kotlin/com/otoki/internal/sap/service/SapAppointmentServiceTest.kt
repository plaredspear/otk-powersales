package com.otoki.internal.sap.service

import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.sap.dto.SapAppointmentRequest.ReqItem
import com.otoki.internal.sap.entity.Appointment
import com.otoki.internal.sap.repository.AppointmentRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAppointmentService 테스트")
class SapAppointmentServiceTest {

    @Mock
    private lateinit var appointmentRepository: AppointmentRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var sapAppointmentService: SapAppointmentService

    @Nested
    @DisplayName("sync - 발령 등록")
    inner class SyncTests {

        @Test
        @DisplayName("사원 존재 - emp_code_exist=true로 Insert")
        fun sync_employeeExists_empCodeExistTrue() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(listOf("100234"))
            whenever(appointmentRepository.save(any<Appointment>()))
                .thenAnswer { it.getArgument<Appointment>(0) }

            val items = listOf(createReqItem(employeeCode = "100234", appointDate = "20260301"))
            val result = sapAppointmentService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<Appointment>()
            verify(appointmentRepository).save(captor.capture())
            assertThat(captor.firstValue.employeeCode).isEqualTo("100234")
            assertThat(captor.firstValue.empCodeExist).isTrue()
            assertThat(captor.firstValue.appointDate).isEqualTo("20260301")
        }

        @Test
        @DisplayName("사원 미존재 - emp_code_exist=false로 Insert")
        fun sync_employeeNotExists_empCodeExistFalse() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(listOf("999999"))
            whenever(appointmentRepository.save(any<Appointment>()))
                .thenAnswer { it.getArgument<Appointment>(0) }

            val items = listOf(createReqItem(employeeCode = "100234", appointDate = "20260301"))
            val result = sapAppointmentService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            val captor = argumentCaptor<Appointment>()
            verify(appointmentRepository).save(captor.capture())
            assertThat(captor.firstValue.empCodeExist).isFalse()
        }

        @Test
        @DisplayName("모든 필드 매핑 확인")
        fun sync_allFieldsMapped() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(emptyList())
            whenever(appointmentRepository.save(any<Appointment>()))
                .thenAnswer { it.getArgument<Appointment>(0) }

            val items = listOf(createReqItem(
                employeeCode = "100234",
                afterOrgCode = "1111",
                afterOrgName = "강남지점",
                jikchak = "지점장",
                jikwee = "과장",
                jikgub = "G3",
                workType = "01",
                manageType = "일반",
                jobCode = "J001",
                workArea = "서울",
                jikjong = "영업",
                appointDate = "20260301",
                jobName = "영업직",
                ordDetailCode = "D001",
                ordDetailNode = "전보"
            ))
            sapAppointmentService.sync(items)

            val captor = argumentCaptor<Appointment>()
            verify(appointmentRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.afterOrgCode).isEqualTo("1111")
            assertThat(saved.afterOrgName).isEqualTo("강남지점")
            assertThat(saved.jikchak).isEqualTo("지점장")
            assertThat(saved.jikwee).isEqualTo("과장")
            assertThat(saved.jikgub).isEqualTo("G3")
            assertThat(saved.workType).isEqualTo("01")
            assertThat(saved.manageType).isEqualTo("일반")
            assertThat(saved.jobCode).isEqualTo("J001")
            assertThat(saved.workArea).isEqualTo("서울")
            assertThat(saved.jikjong).isEqualTo("영업")
            assertThat(saved.jobName).isEqualTo("영업직")
            assertThat(saved.ordDetailCode).isEqualTo("D001")
            assertThat(saved.ordDetailNode).isEqualTo("전보")
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("employee_code 누락 - 해당 레코드 실패")
        fun sync_missingEmployeeCode_fails() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(emptyList())

            val items = listOf(createReqItem(employeeCode = null, appointDate = "20260301"))
            val result = sapAppointmentService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("employee_code")
        }

        @Test
        @DisplayName("appoint_date 누락 - 해당 레코드 실패")
        fun sync_missingAppointDate_fails() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(emptyList())

            val items = listOf(createReqItem(employeeCode = "100234", appointDate = null))
            val result = sapAppointmentService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("appoint_date")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            whenever(userRepository.findAllEmployeeIds()).thenReturn(emptyList())
            whenever(appointmentRepository.save(any<Appointment>()))
                .thenAnswer { it.getArgument<Appointment>(0) }

            val items = listOf(
                createReqItem(employeeCode = "100001", appointDate = "20260301"),
                createReqItem(employeeCode = null, appointDate = "20260301"),
                createReqItem(employeeCode = "100003", appointDate = "20260301")
            )
            val result = sapAppointmentService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        employeeCode: String? = null,
        afterOrgCode: String? = null,
        afterOrgName: String? = null,
        jikchak: String? = null,
        jikwee: String? = null,
        jikgub: String? = null,
        workType: String? = null,
        manageType: String? = null,
        jobCode: String? = null,
        workArea: String? = null,
        jikjong: String? = null,
        appointDate: String? = null,
        jobName: String? = null,
        ordDetailCode: String? = null,
        ordDetailNode: String? = null
    ) = ReqItem(
        employeeCode = employeeCode,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = jikchak,
        jikwee = jikwee,
        jikgub = jikgub,
        workType = workType,
        manageType = manageType,
        jobCode = jobCode,
        workArea = workArea,
        jikjong = jikjong,
        appointDate = appointDate,
        jobName = jobName,
        ordDetailCode = ordDetailCode,
        ordDetailNode = ordDetailNode
    )
}
