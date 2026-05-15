package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterResponse
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.admin.service.AdminEmployeeRegisterService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminEmployeeRegisterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeeRegisterController 테스트")
class AdminEmployeeRegisterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminEmployeeRegisterService: AdminEmployeeRegisterService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.SYSTEM_ADMIN,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    private fun validRequest() = AdminEmployeeRegisterRequest(
        employeeCode = "ADMIN-001",
        name = "홍길동",
        password = "Admin@2026!",
        passwordConfirm = "Admin@2026!",
        workEmail = "admin01@otoki.co.kr",
        workPhone = "02-1234-5678",
        orgName = "본사 IT팀",
        costCenterCode = "IT001"
    )

    private fun stubResponse() = AdminEmployeeRegisterResponse(
        employeeId = 12345L,
        employeeCode = "ADMIN-001",
        name = "홍길동",
        role = UserRole.SYSTEM_ADMIN,
        origin = EmployeeOrigin.MANUAL,
        appLoginActive = false,
        passwordChangeRequired = true,
        createdAt = LocalDateTime.of(2026, 5, 3, 14, 30, 0)
    )

    @Nested
    @DisplayName("POST /api/v1/admin/employees - 시스템 관리자 등록")
    inner class RegisterAdmin {

        @Test
        @DisplayName("성공 - 201 Created, role=SYSTEM_ADMIN, origin=MANUAL")
        fun success() {
            whenever(adminEmployeeRegisterService.register(any())).thenReturn(stubResponse())

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode").value("ADMIN-001"))
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.data.origin").value("MANUAL"))
                .andExpect(jsonPath("$.data.appLoginActive").value(false))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
        }

        @Test
        @DisplayName("권한 부족 - 서비스가 AdminForbiddenException -> 403, error.code=FORBIDDEN")
        fun forbidden() {
            whenever(adminEmployeeRegisterService.register(any()))
                .thenThrow(AdminForbiddenException())

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("필수 필드 누락 - name 누락 -> 400, error.code=INVALID_PARAMETER")
        fun missingName() {
            val invalidJson = """
                {
                  "employeeCode": "ADMIN-001",
                  "password": "Admin@2026!",
                  "passwordConfirm": "Admin@2026!"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("사번 중복 - 서비스가 EmployeeCodeDuplicatedException -> 409")
        fun duplicateCode() {
            whenever(adminEmployeeRegisterService.register(any()))
                .thenThrow(EmployeeCodeDuplicatedException())

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_CODE_DUPLICATED"))
        }
    }
}
