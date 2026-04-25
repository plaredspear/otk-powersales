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
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminEmployeePermissionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeePermissionController 테스트")
class AdminEmployeePermissionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminEmployeePermissionService: AdminEmployeePermissionService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/employees/{employeeId}/permissions - 사용자 권한 조회")
    inner class GetEmployeePermissionsTests {

        @Test
        @DisplayName("성공 - 사원 권한 상세 반환")
        fun success() {
            // Given
            val response = EmployeePermissionDetailResponse(
                employeeId = 2L,
                employeeCode = "00000002",
                name = "홍길동",
                appAuthority = "지점장",
                rolePermissions = listOf("DASHBOARD_READ", "SCHEDULE_READ"),
                userPermissions = listOf(UserPermissionDetailResponse("SCHEDULE_WRITE", "관리자김")),
                effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_READ", "SCHEDULE_WRITE")
            )
            whenever(adminEmployeePermissionService.getEmployeePermissions(2L)).thenReturn(response)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/employees/2/permissions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employee_id").value(2))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.role_permissions[0]").value("DASHBOARD_READ"))
                .andExpect(jsonPath("$.data.user_permissions[0].permission").value("SCHEDULE_WRITE"))
                .andExpect(jsonPath("$.data.user_permissions[0].granted_by_name").value("관리자김"))
                .andExpect(jsonPath("$.data.effective_permissions").isArray)
        }

        @Test
        @DisplayName("실패 - 비관리자 → 403")
        fun forbidden() {
            whenever(adminEmployeePermissionService.getEmployeePermissions(2L))
                .thenThrow(AdminForbiddenException())

            mockMvc.perform(get("/api/v1/admin/employees/2/permissions"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 사원 미존재 → 404")
        fun notFound() {
            whenever(adminEmployeePermissionService.getEmployeePermissions(999L))
                .thenThrow(EmployeeNotFoundException())

            mockMvc.perform(get("/api/v1/admin/employees/999/permissions"))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/employees/{employeeId}/permissions - 사용자 권한 수정")
    inner class UpdateUserPermissionsTests {

        @Test
        @DisplayName("성공 - 권한 수정 후 변경된 상태 반환")
        fun success() {
            // Given
            val response = EmployeePermissionDetailResponse(
                employeeId = 2L, employeeCode = "00000002", name = "홍길동",
                appAuthority = "지점장",
                rolePermissions = listOf("DASHBOARD_READ"),
                userPermissions = listOf(UserPermissionDetailResponse("SCHEDULE_WRITE", "시스템관리자")),
                effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_WRITE")
            )
            whenever(adminEmployeePermissionService.updateUserPermissions(eq(2L), any())).thenReturn(response)

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            // When & Then
            mockMvc.perform(
                put("/api/v1/admin/employees/2/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.effective_permissions").isArray)
        }

        @Test
        @DisplayName("실패 - 자기 자신 수정 → 400")
        fun selfModify() {
            whenever(adminEmployeePermissionService.updateUserPermissions(eq(1L), any()))
                .thenThrow(CannotModifyOwnPermissionException())

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            mockMvc.perform(
                put("/api/v1/admin/employees/1/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_PERMISSION"))
        }

        @Test
        @DisplayName("실패 - 잘못된 권한 → 400")
        fun invalidPermission() {
            whenever(adminEmployeePermissionService.updateUserPermissions(eq(2L), any()))
                .thenThrow(InvalidPermissionException("INVALID"))

            val request = UpdateUserPermissionsRequest(permissions = listOf("INVALID"))

            mockMvc.perform(
                put("/api/v1/admin/employees/2/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PERMISSION"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/employees/{employeeId}/authority - 역할 변경")
    inner class UpdateAuthorityTests {

        @Test
        @DisplayName("성공 - 역할 변경 후 결과 반환")
        fun success() {
            // Given
            val response = UpdateAuthorityResponse(
                employeeId = 2L, employeeCode = "00000002", name = "홍길동",
                previousAuthority = "지점장", newAuthority = "영업부장",
                effectivePermissions = listOf("DASHBOARD_READ")
            )
            whenever(adminEmployeePermissionService.updateAuthority(eq(2L), any())).thenReturn(response)

            val request = UpdateAuthorityRequest(appAuthority = "영업부장")

            // When & Then
            mockMvc.perform(
                put("/api/v1/admin/employees/2/authority")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previous_authority").value("지점장"))
                .andExpect(jsonPath("$.data.new_authority").value("영업부장"))
        }
    }
}
