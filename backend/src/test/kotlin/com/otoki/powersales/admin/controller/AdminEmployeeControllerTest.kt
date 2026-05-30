package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.permission.SfPermissionInspectionService
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.employee.service.AdminEmployeeManualRegisterService
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.employee.service.AdminEmployeeUpdateService
import com.otoki.powersales.schedule.service.EmployeeWorkHistoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeeController 테스트")
class AdminEmployeeControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminEmployeeService: AdminEmployeeService
    @MockkBean private lateinit var adminEmployeeCredentialService: AdminEmployeeCredentialService
    @MockkBean private lateinit var adminEmployeeUpdateService: AdminEmployeeUpdateService
    @MockkBean private lateinit var adminEmployeeManualRegisterService: AdminEmployeeManualRegisterService
    @MockkBean private lateinit var sfPermissionInspectionService: SfPermissionInspectionService
    @MockkBean private lateinit var employeeWorkHistoryService: EmployeeWorkHistoryService

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/employees - 사원 목록 조회")
    inner class GetEmployees {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getEmployees_success() {
            val response = EmployeeListResponse(
                content = listOf(
                    EmployeeListItem(
                        id = 1L,
                        employeeCode = "10000001",
                        name = "홍길동",
                        status = "재직",
                        gender = "남",
                        orgName = "서울1지점",
                        costCenterCode = "A001",
                        role = "조장",
                        
                        startDate = "2020-03-15",
                        endDate = null,
                        appLoginActive = true,
                        workPhone = "010-1234-5678",
                        jikchak = null,
                        jikwee = null,
                        jikgub = null,
                        jobCode = null,
                        appointmentDate = null,
                        ordDetailNode = null,
                        jikjong = null,
                        workEmail = null,
                        phone = null,
                        age = null,
                        yearsOfService = null
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            every { adminEmployeeService.getEmployees(any(), any(), any(), any(), any(), any(), any()) } returns response

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("10000001"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getEmployees_withFilters() {
            val response = EmployeeListResponse(content = emptyList(), page = 0, size = 10, totalElements = 0, totalPages = 0)
            every { adminEmployeeService.getEmployees(any(), eq("재직"), eq("A001"), eq("홍"), eq(AppAuthority.LEADER), eq(0), eq(10)) } returns response

            mockMvc.perform(
                get("/api/v1/admin/employees")
                    .param("status", "재직")
                    .param("costCenterCode", "A001")
                    .param("keyword", "홍")
                    .param("role", "조장")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getEmployees_empty() {
            val response = EmployeeListResponse(content = emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0)
            every { adminEmployeeService.getEmployees(any(), any(), any(), any(), any(), any(), any()) } returns response

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/employees/{id}/reset-device - 단말 초기화 (Spec #582)")
    inner class ResetDevice {

        @Test
        @DisplayName("성공 - 200 OK, deviceUuid 초기화")
        fun resetDevice_success() {
            val response = ResetDeviceResponse(
                employeeId = 12345L,
                employeeCode = "100123",
                name = "홍길동",
                previousDeviceBound = true,
                resetAt = java.time.LocalDateTime.of(2026, 5, 4, 14, 30, 0)
            )
            every { adminEmployeeCredentialService.resetDevice(eq(12345L)) } returns response

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-device"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.employeeId").value(12345L))
                .andExpect(jsonPath("$.data.previousDeviceBound").value(true))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEmployeeControllerTest#resetDeviceExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun resetDevice_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            employeeId: Long,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { adminEmployeeCredentialService.resetDevice(eq(employeeId)) } throws exception

            mockMvc.perform(post("/api/v1/admin/employees/$employeeId/reset-device"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/employees/{id}/reset-password - 비밀번호 임시 리셋 (Spec #582)")
    inner class ResetPassword {

        @Test
        @DisplayName("성공 - 200 OK, passwordChangeRequired=true")
        fun resetPassword_success() {
            val response = ResetPasswordResponse(
                employeeId = 12345L,
                employeeCode = "100123",
                name = "홍길동",
                temporaryPasswordIssued = true,
                passwordChangeRequired = true,
                resetAt = java.time.LocalDateTime.of(2026, 5, 4, 14, 30, 0)
            )
            every { adminEmployeeCredentialService.resetPassword(eq(12345L)) } returns response

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-password"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.employeeId").value(12345L))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEmployeeControllerTest#resetPasswordExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun resetPassword_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            employeeId: Long,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { adminEmployeeCredentialService.resetPassword(eq(employeeId)) } throws exception

            mockMvc.perform(post("/api/v1/admin/employees/$employeeId/reset-password"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun resetDeviceExceptions(): List<Arguments> = listOf(
            Arguments.of("notFound -> 404 EMP_NOT_FOUND", 99999999L, EmployeeNotFoundException(99999999L), 404, "EMP_NOT_FOUND"),
            Arguments.of("loginInactive -> 400 EMP_LOGIN_INACTIVE", 12345L, EmployeeLoginInactiveException(), 400, "EMP_LOGIN_INACTIVE"),
        )

        @JvmStatic
        fun resetPasswordExceptions(): List<Arguments> = listOf(
            Arguments.of("notFound -> 404 EMP_NOT_FOUND", 99999999L, EmployeeNotFoundException(99999999L), 404, "EMP_NOT_FOUND"),
            Arguments.of("loginInactive -> 400 EMP_LOGIN_INACTIVE", 12345L, EmployeeLoginInactiveException(), 400, "EMP_LOGIN_INACTIVE"),
        )
    }
}
