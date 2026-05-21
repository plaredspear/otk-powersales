package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.*
import com.otoki.powersales.admin.service.AdminPermissionMatrixService
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.user.entity.ProfileType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPermissionMatrixController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPermissionMatrixController 테스트")
class AdminPermissionMatrixControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminPermissionMatrixService: AdminPermissionMatrixService

    @MockkBean
    private lateinit var adminEmployeePermissionService: com.otoki.powersales.admin.service.AdminEmployeePermissionService

    @BeforeEach
    fun setUpLeaderPrincipal() {
        authenticateAsAdmin(
            role = UserRoleEnum.LEADER,
            costCenterCode = "1234",
            profileType = ProfileType.TEAM_LEADER,
        )
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
                    RolePermissions("LEADER", "조장", listOf("DASHBOARD_READ", "EMPLOYEE_READ")),
                    RolePermissions("BRANCH_MANAGER", "지점장", listOf("DASHBOARD_READ", "EMPLOYEE_READ"))
                ),
                currentUser = CurrentUserPermission("LEADER", "조장", listOf("DASHBOARD_READ", "EMPLOYEE_READ"))
            )
            every { adminPermissionMatrixService.getMatrix(1L) } returns mockResponse

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
                .andExpect(jsonPath("$.data.roles[0].role").value("LEADER"))
                .andExpect(jsonPath("$.data.roles[0].roleLabel").value("조장"))
                .andExpect(jsonPath("$.data.roles[0].permissions[0]").value("DASHBOARD_READ"))
                .andExpect(jsonPath("$.data.currentUser.role").value("LEADER"))
                .andExpect(jsonPath("$.data.currentUser.roleLabel").value("조장"))
                .andExpect(jsonPath("$.data.currentUser.permissions").isArray)
        }
    }
}
