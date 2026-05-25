package com.otoki.powersales.admin.userrole

import com.otoki.powersales.admin.userrole.dto.UserRoleNode
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminUserRoleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminUserRoleController 테스트")
class AdminUserRoleControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var service: AdminUserRoleService

    @BeforeEach
    fun setUpPrincipal() {
        authenticateAsAdmin(role = null)
    }

    @Test
    @DisplayName("GET /tree - 부모-자식 hierarchy 반환")
    fun getTree() {
        every { service.getTree() } returns listOf(
            UserRoleNode(
                userRoleId = 1,
                name = "회장님",
                developerName = "CEO",
                rollupDescription = "회장님",
                parentUserRoleId = null,
                parentName = null,
                children = listOf(
                    UserRoleNode(
                        userRoleId = 2,
                        name = "사장님",
                        developerName = "representative",
                        rollupDescription = "사장님",
                        parentUserRoleId = 1,
                        parentName = "회장님",
                        children = emptyList(),
                    ),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/admin/user-roles/tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].userRoleId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("회장님"))
            .andExpect(jsonPath("$.data[0].developerName").value("CEO"))
            .andExpect(jsonPath("$.data[0].children[0].userRoleId").value(2))
            .andExpect(jsonPath("$.data[0].children[0].parentName").value("회장님"))
    }
}
