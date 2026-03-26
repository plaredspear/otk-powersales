package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminPermissionMatrixService
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPermissionMatrixController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPermissionMatrixController 테스트")
class AdminPermissionMatrixControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminPermissionMatrixService: AdminPermissionMatrixService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.LEADER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/permissions/matrix - 역할-권한 매트릭스 조회")
    inner class GetMatrixTests {

        @Test
        @DisplayName("성공 - permissions, roles, current_user 포함 응답")
        fun getMatrix_success() {
            // Given
            val mockResponse = PermissionMatrixResponse(
                permissions = listOf(
                    PermissionDetail("DASHBOARD_READ", "대시보드 조회", listOf("대시보드")),
                    PermissionDetail("EMPLOYEE_READ", "사원 조회", listOf("사원 > 여사원 현황"))
                ),
                roles = listOf(
                    RolePermissions("조장", listOf("DASHBOARD_READ", "EMPLOYEE_READ")),
                    RolePermissions("지점장", listOf("DASHBOARD_READ", "EMPLOYEE_READ"))
                ),
                currentUser = CurrentUserPermission("조장", listOf("DASHBOARD_READ", "EMPLOYEE_READ"))
            )
            whenever(adminPermissionMatrixService.getMatrix(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/permissions/matrix"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.permissions").isArray)
                .andExpect(jsonPath("$.data.permissions.length()").value(2))
                .andExpect(jsonPath("$.data.permissions[0].code").value("DASHBOARD_READ"))
                .andExpect(jsonPath("$.data.permissions[0].description").value("대시보드 조회"))
                .andExpect(jsonPath("$.data.permissions[0].menus[0]").value("대시보드"))
                .andExpect(jsonPath("$.data.roles").isArray)
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.roles[0].role").value("조장"))
                .andExpect(jsonPath("$.data.roles[0].permissions[0]").value("DASHBOARD_READ"))
                .andExpect(jsonPath("$.data.current_user.role").value("조장"))
                .andExpect(jsonPath("$.data.current_user.permissions").isArray)
        }
    }
}
