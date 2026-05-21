package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.request.UpdateAuthorityRequest
import com.otoki.powersales.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.powersales.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.powersales.admin.dto.response.UpdateAuthorityResponse
import com.otoki.powersales.admin.dto.response.UserPermissionDetailResponse
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.CannotModifyOwnPermissionException
import com.otoki.powersales.admin.exception.InvalidPermissionException
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminEmployeePermissionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeePermissionController 테스트")
class AdminEmployeePermissionControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminEmployeePermissionService: AdminEmployeePermissionService
    @MockkBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockkBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockkBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            costCenterCode = "1234",
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/employees/{employeeId}/permissions - 사용자 권한 조회")
    inner class GetEmployeePermissionsTests {

        @Test
        @DisplayName("성공 - 사원 권한 상세 반환")
        fun success() {
            val response = EmployeePermissionDetailResponse(
                employeeId = 2L,
                employeeCode = "00000002",
                name = "홍길동",
                role = "BRANCH_MANAGER",
                roleLabel = "지점장",
                rolePermissions = listOf("DASHBOARD_READ", "SCHEDULE_READ"),
                userPermissions = listOf(UserPermissionDetailResponse("SCHEDULE_WRITE")),
                effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_READ", "SCHEDULE_WRITE")
            )
            every { adminEmployeePermissionService.getEmployeePermissions(any(), eq(2L)) } returns response

            mockMvc.perform(get("/api/v1/admin/employees/2/permissions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.employeeId").value(2))
                .andExpect(jsonPath("$.data.rolePermissions[0]").value("DASHBOARD_READ"))
                .andExpect(jsonPath("$.data.userPermissions[0].permission").value("SCHEDULE_WRITE"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEmployeePermissionControllerTest#getExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun get_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            employeeId: Long,
            exception: Throwable,
            expectedStatus: Int,
        ) {
            every { adminEmployeePermissionService.getEmployeePermissions(any(), eq(employeeId)) } throws exception

            mockMvc.perform(get("/api/v1/admin/employees/$employeeId/permissions"))
                .andExpect(status().`is`(expectedStatus))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/employees/{employeeId}/permissions - 사용자 권한 수정")
    inner class UpdateUserPermissionsTests {

        @Test
        @DisplayName("성공 - 권한 수정 후 변경된 상태 반환")
        fun success() {
            val response = EmployeePermissionDetailResponse(
                employeeId = 2L, employeeCode = "00000002", name = "홍길동",
                role = "BRANCH_MANAGER",
                roleLabel = "지점장",
                rolePermissions = listOf("DASHBOARD_READ"),
                userPermissions = listOf(UserPermissionDetailResponse("SCHEDULE_WRITE")),
                effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_WRITE")
            )
            every { adminEmployeePermissionService.updateUserPermissions(any(), eq(2L), any()) } returns response

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            mockMvc.perform(
                put("/api/v1/admin/employees/2/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.effectivePermissions").isArray)
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEmployeePermissionControllerTest#updateExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun update_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            employeeId: Long,
            exception: Throwable,
            expectedCode: String,
        ) {
            every { adminEmployeePermissionService.updateUserPermissions(any(), eq(employeeId), any()) } throws exception

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            mockMvc.perform(
                put("/api/v1/admin/employees/$employeeId/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/employees/{employeeId}/authority - 역할 변경")
    inner class UpdateAuthorityTests {

        @Test
        @DisplayName("성공 - 역할 변경 후 결과 반환")
        fun success() {
            val response = UpdateAuthorityResponse(
                employeeId = 2L, employeeCode = "00000002", name = "홍길동",
                previousRole = "BRANCH_MANAGER", previousRoleLabel = "지점장",
                newRole = "SALES_MANAGER", newRoleLabel = "영업부장",
                effectivePermissions = listOf("DASHBOARD_READ")
            )
            every { adminEmployeePermissionService.updateAuthority(any(), eq(2L), any()) } returns response

            val request = UpdateAuthorityRequest(role = UserRole.SALES_MANAGER)

            mockMvc.perform(
                put("/api/v1/admin/employees/2/authority")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.previousRole").value("BRANCH_MANAGER"))
                .andExpect(jsonPath("$.data.newRole").value("SALES_MANAGER"))
        }
    }

    companion object {
        @JvmStatic
        fun getExceptions(): List<Arguments> = listOf(
            Arguments.of("forbidden -> 403 FORBIDDEN", 2L, AdminForbiddenException(), 403),
            Arguments.of("notFound -> 404", 999L, EmployeeNotFoundException(), 404),
        )

        @JvmStatic
        fun updateExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "selfModify -> 400 CANNOT_MODIFY_OWN_PERMISSION",
                1L,
                CannotModifyOwnPermissionException(),
                "CANNOT_MODIFY_OWN_PERMISSION",
            ),
            Arguments.of(
                "invalidPermission -> 400 INVALID_PERMISSION",
                2L,
                InvalidPermissionException("INVALID"),
                "INVALID_PERMISSION",
            ),
        )
    }
}
