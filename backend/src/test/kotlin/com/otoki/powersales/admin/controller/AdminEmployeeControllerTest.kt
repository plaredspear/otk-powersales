package com.otoki.powersales.admin.controller

import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeeController 테스트")
class AdminEmployeeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminEmployeeService: AdminEmployeeService

    @MockitoBean
    private lateinit var adminEmployeeCredentialService: AdminEmployeeCredentialService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    // @AutoConfigureMockMvc(addFilters = false) 환경에서 WebAdminContextFilter 가 동작하지 않으므로
    // ArgumentResolver 자체를 stub 하여 ALL scope 기본값 주입.
    @MockitoBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        whenever(currentAdminContextArgumentResolver.supportsParameter(any())).thenAnswer { invocation ->
            val parameter = invocation.arguments[0] as MethodParameter
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        whenever(currentAdminContextArgumentResolver.resolveArgument(any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(DataScope(branchCodes = emptyList(), isAllBranches = true))
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
                        role = "LEADER",
                        roleLabel = "조장",
                        startDate = "2020-03-15",
                        endDate = null,
                        appLoginActive = true,
                        workPhone = "010-1234-5678",
                        jikchak = null,
                        jikwee = null,
                        jikgub = null,
                        jobCode = null,
                        appointmentDate = null,
                        ordDetailNode = null
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminEmployeeService.getEmployees(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("10000001"))
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].status").value("재직"))
                .andExpect(jsonPath("$.data.content[0].gender").value("남"))
                .andExpect(jsonPath("$.data.content[0].orgName").value("서울1지점"))
                .andExpect(jsonPath("$.data.content[0].costCenterCode").value("A001"))
                .andExpect(jsonPath("$.data.content[0].role").value("LEADER"))
                .andExpect(jsonPath("$.data.content[0].roleLabel").value("조장"))
                .andExpect(jsonPath("$.data.content[0].startDate").value("2020-03-15"))
                .andExpect(jsonPath("$.data.content[0].appLoginActive").value(true))
                .andExpect(jsonPath("$.data.content[0].workPhone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getEmployees_withFilters() {
            val response = EmployeeListResponse(
                content = emptyList(),
                page = 0,
                size = 10,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminEmployeeService.getEmployees(any(), eq("재직"), eq("A001"), eq("홍"), eq(UserRole.LEADER), eq(0), eq(10)))
                .thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/employees")
                    .param("status", "재직")
                    .param("costCenterCode", "A001")
                    .param("keyword", "홍")
                    .param("role", "LEADER")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getEmployees_empty() {
            val response = EmployeeListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminEmployeeService.getEmployees(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
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
                resetAt = LocalDateTime.of(2026, 5, 4, 14, 30, 0)
            )
            whenever(adminEmployeeCredentialService.resetDevice(eq(12345L))).thenReturn(response)

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-device"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(12345L))
                .andExpect(jsonPath("$.data.employeeCode").value("100123"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.previousDeviceBound").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 사원 -> 404, EMP_NOT_FOUND")
        fun resetDevice_notFound() {
            whenever(adminEmployeeCredentialService.resetDevice(eq(99999999L)))
                .thenThrow(EmployeeNotFoundException(99999999L))

            mockMvc.perform(post("/api/v1/admin/employees/99999999/reset-device"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("EMP_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 앱 로그인 비활성 사원 -> 400, EMP_LOGIN_INACTIVE")
        fun resetDevice_loginInactive() {
            whenever(adminEmployeeCredentialService.resetDevice(eq(12345L)))
                .thenThrow(EmployeeLoginInactiveException())

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-device"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMP_LOGIN_INACTIVE"))
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
                resetAt = LocalDateTime.of(2026, 5, 4, 14, 30, 0)
            )
            whenever(adminEmployeeCredentialService.resetPassword(eq(12345L))).thenReturn(response)

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-password"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(12345L))
                .andExpect(jsonPath("$.data.temporaryPasswordIssued").value(true))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 사원 -> 404, EMP_NOT_FOUND")
        fun resetPassword_notFound() {
            whenever(adminEmployeeCredentialService.resetPassword(eq(99999999L)))
                .thenThrow(EmployeeNotFoundException(99999999L))

            mockMvc.perform(post("/api/v1/admin/employees/99999999/reset-password"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("EMP_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 앱 로그인 비활성 사원 -> 400, EMP_LOGIN_INACTIVE")
        fun resetPassword_loginInactive() {
            whenever(adminEmployeeCredentialService.resetPassword(eq(12345L)))
                .thenThrow(EmployeeLoginInactiveException())

            mockMvc.perform(post("/api/v1/admin/employees/12345/reset-password"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMP_LOGIN_INACTIVE"))
        }
    }
}
