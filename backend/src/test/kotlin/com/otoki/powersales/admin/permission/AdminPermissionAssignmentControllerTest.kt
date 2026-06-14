package com.otoki.powersales.admin.permission

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.permission.dto.AssignmentBatchItem
import com.otoki.powersales.admin.permission.dto.AssignmentBatchResult
import com.otoki.powersales.admin.permission.dto.AssignmentResponse
import com.otoki.powersales.admin.permission.exception.AssignmentAlreadyExistsException
import com.otoki.powersales.admin.permission.exception.CannotRevokeSelfException
import com.otoki.powersales.admin.permission.exception.LastAdminGuardException
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminPermissionAssignmentController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPermissionAssignmentController 테스트")
class AdminPermissionAssignmentControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var assignmentService: AdminPermissionAssignmentService

    @BeforeEach
    fun setUpPrincipal() {
        authenticateAsAdmin(role = null)
    }

    @Test
    @DisplayName("POST /assignments - 신규 부여 → 201")
    fun assignSuccess() {
        every { assignmentService.assign(100L, 11L, 100L) } returns AssignmentResponse(
            assignmentId = 5001,
            userId = 100,
            permissionSetFlagsId = 11,
            isActive = true,
            assignedAt = LocalDateTime.now(),
            createdById = 100,
        )

        mockMvc.perform(
            post("/api/v1/admin/permissions/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":100,"permissionSetFlagsId":11}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.assignmentId").value(5001))
            .andExpect(jsonPath("$.data.isActive").value(true))
    }

    @Test
    @DisplayName("POST /assignments - 동일 (user, ps) active 존재 → 409")
    fun assignAlreadyExists() {
        every { assignmentService.assign(100L, 11L, 100L) } throws AssignmentAlreadyExistsException(100L, 11L)

        mockMvc.perform(
            post("/api/v1/admin/permissions/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":100,"permissionSetFlagsId":11}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_ALREADY_EXISTS"))
    }

    @Test
    @DisplayName("DELETE /assignments/{id} - 정상 회수 → 204")
    fun revokeSuccess() {
        every { assignmentService.revoke(5001L, 100L) } just runs

        mockMvc.perform(delete("/api/v1/admin/permissions/assignments/5001"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { assignmentService.revoke(5001L, 100L) }
    }

    @Test
    @DisplayName("DELETE /assignments/{id} - self-revoke → 400")
    fun revokeSelfBlocked() {
        every { assignmentService.revoke(5001L, 100L) } throws CannotRevokeSelfException()

        mockMvc.perform(delete("/api/v1/admin/permissions/assignments/5001"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("CANNOT_REVOKE_SELF"))
    }

    @Test
    @DisplayName("DELETE /assignments/{id} - last-admin → 400")
    fun revokeLastAdminBlocked() {
        every { assignmentService.revoke(5001L, 100L) } throws LastAdminGuardException()

        mockMvc.perform(delete("/api/v1/admin/permissions/assignments/5001"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("LAST_ADMIN_GUARD"))
    }

    @Test
    @DisplayName("POST /assignments/batch - Mode A 부분 성공")
    fun batchAssignPartial() {
        every { assignmentService.assignBatch(any(), 100L) } returns AssignmentBatchResult(
            succeeded = listOf(AssignmentBatchItem(100, 11, assignmentId = 5001)),
            skipped = listOf(AssignmentBatchItem(100, 12, reason = "ASSIGNMENT_ALREADY_EXISTS")),
            failed = emptyList(),
        )

        mockMvc.perform(
            post("/api/v1/admin/permissions/assignments/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":100,"permissionSetFlagsIds":[11,12]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.succeeded[0].assignmentId").value(5001))
            .andExpect(jsonPath("$.data.skipped[0].reason").value("ASSIGNMENT_ALREADY_EXISTS"))
    }
}
