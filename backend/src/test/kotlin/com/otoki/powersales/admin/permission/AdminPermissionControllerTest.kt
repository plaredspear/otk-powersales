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
import com.otoki.powersales.admin.permission.dto.PermissionSetMatrix
import com.otoki.powersales.admin.permission.dto.PermissionSetMatrixEntry
import com.otoki.powersales.admin.permission.dto.PermissionSetSummary
import com.otoki.powersales.admin.permission.dto.ProfileDetail
import com.otoki.powersales.admin.permission.dto.ProfileFlagsSummary
import com.otoki.powersales.admin.permission.dto.ProfileSummary
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.platform.auth.permission.EntitySfNameRegistry
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

    @MockkBean
    private lateinit var mutationService: AdminPermissionSetMutationService

    @MockkBean
    private lateinit var profileFlagsMutationService: AdminProfileFlagsMutationService

    @MockkBean
    private lateinit var entitySfNameRegistry: EntitySfNameRegistry

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
            flags = ProfileFlagsSummary(
                viewAllData = true, modifyAllData = true, viewAllUsers = true, manageUsers = true, apiEnabled = true,
            ),
            objectPermissions = listOf(
                ObjectPermissionRow(
                    sfApiName = "MonthlySalesHistory__c", entity = "monthly_sales_history",
                    canRead = true, canCreate = false, canEdit = false, canDelete = false,
                ),
            ),
            customPermissions = emptyList(),
            isLocallyModified = true,
            assignedUsers = PaginatedUserList(
                totalElements = 1, totalPages = 1, number = 0, size = 20,
                content = listOf(AssignedUserSummary(userId = 100, username = "u@x", employeeCode = "S001", employeeName = "홍관리")),
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/profiles/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.profileId").value(1))
            .andExpect(jsonPath("$.data.flags.modifyAllData").value(true))
            .andExpect(jsonPath("$.data.objectPermissions[0].entity").value("monthly_sales_history"))
            .andExpect(jsonPath("$.data.objectPermissions[0].canRead").value(true))
            .andExpect(jsonPath("$.data.isLocallyModified").value(true))
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
                sfOrigin = true,
                isLocallyModified = false,
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
            flags = PermissionSetFlagsSummary(permissionSetFlagsId = 11, viewAllData = false, modifyAllData = false),
            objectPermissions = listOf(
                ObjectPermissionRow(sfApiName = "Account", entity = "account", canRead = true, canCreate = true, canEdit = true, canDelete = false),
            ),
            customPermissions = emptyList(),
            assignedUsers = PaginatedPermissionSetUserList(0, 0, 0, 20, emptyList()),
            sfOrigin = true,
            isLocallyModified = false,
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
    @DisplayName("GET /permission-sets/matrix - PermissionSet 매트릭스 일괄 반환")
    fun getPermissionSetMatrix() {
        every { inspectionService.getPermissionSetMatrix() } returns PermissionSetMatrix(
            permissionSets = listOf(
                PermissionSetMatrixEntry(
                    permissionSetId = 10,
                    name = "AccountManagement",
                    label = "거래처 관리",
                    viewAllData = false,
                    modifyAllData = false,
                    objectPermissions = listOf(
                        ObjectPermissionRow(
                            sfApiName = "Account", entity = "account",
                            canRead = true, canCreate = true, canEdit = false, canDelete = false,
                        ),
                    ),
                ),
                PermissionSetMatrixEntry(
                    permissionSetId = 11,
                    name = "SystemAdmin",
                    label = null,
                    viewAllData = true,
                    modifyAllData = true,
                    objectPermissions = emptyList(),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets/matrix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.permissionSets[0].permissionSetId").value(10))
            .andExpect(jsonPath("$.data.permissionSets[0].label").value("거래처 관리"))
            .andExpect(jsonPath("$.data.permissionSets[0].objectPermissions[0].entity").value("account"))
            .andExpect(jsonPath("$.data.permissionSets[0].objectPermissions[0].canRead").value(true))
            .andExpect(jsonPath("$.data.permissionSets[1].modifyAllData").value(true))
            .andExpect(jsonPath("$.data.permissionSets[1].objectPermissions").isArray)
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

    // ── Spec #837 — PermissionSet 자체 관리 endpoint 테스트 ──────────────────

    @Test
    @DisplayName("GET /permission-sets/available-resources - SF + custom 자원 카탈로그 반환")
    fun listAvailableResources() {
        io.mockk.every { entitySfNameRegistry.snapshot() } returns mapOf(
            "account" to "Account",
            "claim" to "DKRetail__Claim__c",
        )
        io.mockk.every { entitySfNameRegistry.allResources() } returns setOf("account", "claim", "dashboard")

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets/available-resources"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sfObjects.length()").value(2))
            .andExpect(jsonPath("$.data.sfObjects[0].sfApiName").value("Account"))
            .andExpect(jsonPath("$.data.customResources[0]").value("dashboard"))
    }

    @Test
    @DisplayName("POST /permission-sets - 신규 생성 → 201")
    fun createPermissionSet() {
        every { mutationService.create(any(), 100L) } returns com.otoki.powersales.admin.permission.dto.PermissionSetMutationResponse(
            permissionSetId = 90,
            name = "신규_조회_권한",
            label = "신규 조회 권한",
            description = null,
            sfOrigin = false,
            permissionSetFlagsId = 90,
            viewAllData = false,
            modifyAllData = false,
            objectPermissions = emptyMap(),
            customPermissions = emptyMap(),
            isLocallyModified = false,
        )

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/admin/permissions/permission-sets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""{"name":"신규_조회_권한","label":"신규 조회 권한"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.permissionSetId").value(90))
            .andExpect(jsonPath("$.data.sfOrigin").value(false))
            .andExpect(jsonPath("$.data.isLocallyModified").value(false))
    }

    @Test
    @DisplayName("POST /permission-sets - name 중복 → 409")
    fun createPermissionSetNameConflict() {
        every { mutationService.create(any(), 100L) } throws
            com.otoki.powersales.admin.permission.exception.PermissionSetNameAlreadyExistsException("AccountManagement")

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/admin/permissions/permission-sets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""{"name":"AccountManagement"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("NAME_ALREADY_EXISTS"))
    }

    @Test
    @DisplayName("PUT /permission-sets/{id}/flags - 권한 비트 갱신 → 200")
    fun updatePermissionSetFlags() {
        every { mutationService.updateFlags(90L, any(), 100L) } returns com.otoki.powersales.admin.permission.dto.PermissionSetMutationResponse(
            permissionSetId = 90,
            name = "신규_조회_권한",
            label = null,
            description = null,
            sfOrigin = false,
            permissionSetFlagsId = 90,
            viewAllData = false,
            modifyAllData = false,
            objectPermissions = mapOf("Account" to mapOf("allowRead" to true)),
            customPermissions = emptyMap(),
            isLocallyModified = false,
        )

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/admin/permissions/permission-sets/90/flags")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""{"viewAllData":false,"modifyAllData":false,"objectPermissions":{"Account":{"allowRead":true}},"customPermissions":{}}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.objectPermissions.Account.allowRead").value(true))
    }

    @Test
    @DisplayName("DELETE /permission-sets/{id} - SF 출처 PS 삭제 시도 → 409")
    fun deleteSfOriginBlocked() {
        every { mutationService.delete(90L, 100L) } throws
            com.otoki.powersales.admin.permission.exception.SfOriginDeleteBlockedException(90L)

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/admin/permissions/permission-sets/90")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("SF_ORIGIN_DELETE_BLOCKED"))
    }

    @Test
    @DisplayName("DELETE /permission-sets/{id} - 신규 PS 정상 삭제 → 204")
    fun deletePermissionSetSuccess() {
        io.mockk.every { mutationService.delete(90L, 100L) } returns Unit

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/admin/permissions/permission-sets/90")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("GET /permission-sets/{id}/change-log - 변경 이력 페이지네이션")
    fun listChangeLog() {
        every { mutationService.listChangeLog(90L, 0, 20) } returns org.springframework.data.domain.PageImpl(
            listOf(
                com.otoki.powersales.admin.permission.dto.PermissionSetChangeLogResponse(
                    changeLogId = 1,
                    permissionSetId = 90,
                    eventType = "CREATE",
                    beforeSnapshot = null,
                    afterSnapshot = """{"name":"신규_조회_권한"}""",
                    changedById = 100,
                    changedByName = "관리자",
                    changedAt = java.time.LocalDateTime.now(),
                    changeReason = null,
                ),
            ),
            org.springframework.data.domain.PageRequest.of(0, 20),
            1,
        )

        mockMvc.perform(get("/api/v1/admin/permissions/permission-sets/90/change-log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].eventType").value("CREATE"))
    }
}
