package com.otoki.powersales.admin.permission.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * Spec #804 — Assignment 부여/회수 endpoint 의 request/response DTO.
 */

data class AssignmentCreateRequest(
    @field:NotNull
    val userId: Long,

    @field:NotNull
    val permissionSetFlagsId: Long,
)

/**
 * 일괄 부여 — Mode A (한 user × 다수 ps) 또는 Mode B (다수 user × 한 ps).
 * 둘 다 동시 지정 시 service 가 400 으로 거부 (Controller 입력 검증).
 */
data class AssignmentBatchRequest(
    val userId: Long? = null,
    val permissionSetFlagsId: Long? = null,
    val userIds: List<Long>? = null,
    val permissionSetFlagsIds: List<Long>? = null,
)

data class AssignmentResponse(
    val assignmentId: Long,
    val userId: Long,
    val permissionSetFlagsId: Long,
    val isActive: Boolean,
    val assignedAt: LocalDateTime?,
    val createdById: Long?,
)

data class AssignmentBatchItem(
    val userId: Long,
    val permissionSetFlagsId: Long,
    val assignmentId: Long? = null,
    val reason: String? = null,
)

data class AssignmentBatchResult(
    val succeeded: List<AssignmentBatchItem>,
    val skipped: List<AssignmentBatchItem>,
    val failed: List<AssignmentBatchItem>,
)
