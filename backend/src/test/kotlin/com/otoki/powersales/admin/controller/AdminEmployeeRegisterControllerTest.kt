package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterResponse
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.service.AdminEmployeeRegisterService
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.employee.enums.EmployeeOrigin
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminEmployeeRegisterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeeRegisterController 테스트")
class AdminEmployeeRegisterControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminEmployeeRegisterService: AdminEmployeeRegisterService

    @BeforeEach
    fun setUpSystemAdminPrincipal() {
        authenticateAsAdmin(role = null)
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
        role = null,
        origin = EmployeeOrigin.MANUAL,
        appLoginActive = false,
        passwordChangeRequired = true,
        createdAt = java.time.LocalDateTime.of(2026, 5, 3, 14, 30, 0)
    )

    @Nested
    @DisplayName("POST /api/v1/admin/employees - 시스템 관리자 등록")
    inner class RegisterAdmin {

        @Test
        @DisplayName("성공 - 201 Created, role=null (시스템 관리자 — AppAuthority 부재), origin=MANUAL")
        fun success() {
            every { adminEmployeeRegisterService.register(any(), any()) } returns stubResponse()

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.origin").value("MANUAL"))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
        }

        @Test
        @DisplayName("필수 필드 누락 - name 누락 -> 400")
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

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEmployeeRegisterControllerTest#registerExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun register_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { adminEmployeeRegisterService.register(any(), any()) } throws exception

            mockMvc.perform(
                post("/api/v1/admin/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun registerExceptions(): List<Arguments> = listOf(
            Arguments.of("forbidden -> 403 FORBIDDEN", AdminForbiddenException(), 403, "FORBIDDEN"),
            Arguments.of(
                "duplicatedCode -> 409 EMPLOYEE_CODE_DUPLICATED",
                EmployeeCodeDuplicatedException(),
                409,
                "EMPLOYEE_CODE_DUPLICATED",
            ),
        )
    }
}
