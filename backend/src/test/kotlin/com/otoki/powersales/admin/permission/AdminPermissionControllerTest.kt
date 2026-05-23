package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AssignedUserSummary
import com.otoki.powersales.admin.permission.dto.EntityProfilePermission
import com.otoki.powersales.admin.permission.dto.PaginatedPermissionSetUserList
import com.otoki.powersales.admin.permission.dto.EntityProfileRow
import com.otoki.powersales.admin.permission.dto.ObjectPermissionRow
import com.otoki.powersales.admin.permission.dto.PaginatedUserList
import com.otoki.powersales.admin.permission.dto.PermissionMatrix
import com.otoki.powersales.admin.permission.dto.PermissionMatrixProfile
import com.otoki.powersales.admin.permission.dto.PermissionSetDetail
import com.otoki.powersales.admin.permission.dto.PermissionSetFlagsSummary
import com.otoki.powersales.admin.permission.dto.PermissionSetSummary
import com.otoki.powersales.admin.permission.dto.ProfileDetail
import com.otoki.powersales.admin.permission.dto.ProfileFlagsSummary
import com.otoki.powersales.admin.permission.dto.ProfileSummary
import com.otoki.powersales.auth.entity.AppAuthority
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

@WebMvcTest(AdminPermissionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPermissionController 테스트")
class AdminPermissionControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var inspectionService: AdminPermissionInspectionService

    @BeforeEach
    fun setUpPrincipal() {
        authenticateAsAdmin(role = null)
    }

    @Test
    @DisplayName("GET /profiles - 일람 반환")
    fun listProfiles() {
        every { inspectionService.listProfiles() } returns listOf(
            ProfileSummary(
                profileId = 1,
                name = "System Administrator",
                userType = "Standard",
                description = "관리자",
                viewAllData = true,
                modifyAllData = true,
                viewAllUsers = true,
                manageUsers = true,
                apiEnabled = true,
                assignedUserCount = 3,
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/profiles"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].profileId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("System Administrator"))
            .andExpect(jsonPath("$.data[0].modifyAllData").value(true))
            .andExpect(jsonPath("$.data[0].assignedUserCount").value(3))
    }

    @Test
    @DisplayName("GET /profiles/{id} - 정상 상세")
    fun getProfile() {
        every { inspectionService.getProfile(1, 0, 20, null) } returns ProfileDetail(
            profileId = 1,
            name = "System Administrator",
            userType = "Standard",
            description = null,
            sfid = "00e3z00000G1abc",
            flags = ProfileFlagsSummary(
                viewAllData = true, modifyAllData = true, viewAllUsers = true, manageUsers = true, apiEnabled = true,
            ),
            assignedUsers = PaginatedUserList(
                totalElements = 1, totalPages = 1, number = 0, size = 20,
                content = listOf(AssignedUserSummary(userId = 100, username = "u@x", employeeCode = "S001", employeeName = "홍관리")),
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/profiles/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.profileId").value(1))
            .andExpect(jsonPath("$.data.flags.modifyAllData").value(true))
            .andExpect(jsonPath("$.data.assignedUsers.content[0].employeeName").value("홍관리"))
    }

    @Test
    @DisplayName("GET /profiles/{id} - 미존재 시 404")
    fun getProfileNotFound() {
        every { inspectionService.getProfile(99, 0, 20, null) } returns null

        mockMvc.perform(get("/api/v1/admin/permissions/profiles/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_FOUND"))
    }

    @Test
    @DisplayName("GET /permission-sets - 일람 반환")
    fun listPermissionSets() {
        every { inspectionService.listPermissionSets() } returns listOf(
            PermissionSetSummary(
                permissionSetId = 10,
                name = "AccountManagement",
                label = "거래처 관리",
                description = "거래처 CRUD",
                permissionSetFlagsId = 11,
                viewAllData = false,
                modifyAllData = false,
                objectPermissionCount = 5,
                assignedUserCount = 12,
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].permissionSetId").value(10))
            .andExpect(jsonPath("$.data[0].objectPermissionCount").value(5))
    }

    @Test
    @DisplayName("GET /permission-sets/{id} - 정상 상세")
    fun getPermissionSet() {
        every { inspectionService.getPermissionSet(10, 0, 20, null) } returns PermissionSetDetail(
            permissionSetId = 10,
            name = "AccountManagement",
            label = "거래처 관리",
            description = null,
            sfid = "0PS3z00000A1abc",
            flags = PermissionSetFlagsSummary(permissionSetFlagsId = 11, viewAllData = false, modifyAllData = false),
            objectPermissions = listOf(
                ObjectPermissionRow(sfApiName = "Account", entity = "account", canRead = true, canCreate = true, canEdit = true, canDelete = false),
            ),
            assignedUsers = PaginatedPermissionSetUserList(0, 0, 0, 20, emptyList()),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.permissionSetId").value(10))
            .andExpect(jsonPath("$.data.objectPermissions[0].entity").value("account"))
            .andExpect(jsonPath("$.data.objectPermissions[0].canDelete").value(false))
    }

    @Test
    @DisplayName("GET /permission-sets/{id} - 미존재 시 404")
    fun getPermissionSetNotFound() {
        every { inspectionService.getPermissionSet(99, 0, 20, null) } returns null

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("PERMISSION_SET_NOT_FOUND"))
    }

    @Test
    @DisplayName("GET /matrix - 매트릭스 반환")
    fun getMatrix() {
        every { inspectionService.getMatrix() } returns PermissionMatrix(
            profiles = listOf(PermissionMatrixProfile(1, "System Administrator")),
            rows = listOf(
                EntityProfileRow(
                    entity = "account",
                    byProfile = listOf(EntityProfilePermission(profileId = 1, canRead = true, canCreate = true, canEdit = true, canDelete = true)),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/matrix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.profiles[0].profileId").value(1))
            .andExpect(jsonPath("$.data.rows[0].entity").value("account"))
            .andExpect(jsonPath("$.data.rows[0].byProfile[0].canDelete").value(true))
    }
}
