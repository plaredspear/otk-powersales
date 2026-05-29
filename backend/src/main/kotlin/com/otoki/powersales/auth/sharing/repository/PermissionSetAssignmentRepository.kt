package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSetAssignment
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetAssignmentRepository : JpaRepository<PermissionSetAssignment, Long> {
    fun findAllByAssigneeUserIdAndIsActiveTrue(assigneeUserId: Long): List<PermissionSetAssignment>

    fun countByPermissionSetFlagsIdAndIsActiveTrue(permissionSetFlagsId: Long): Long

    fun findAllByPermissionSetFlagsIdAndIsActiveTrue(permissionSetFlagsId: Long): List<PermissionSetAssignment>

    /** Spec #804 — 동일 (user, ps) 중복 부여 검사 + 재부여 시 inactive row lookup. */
    fun findByAssigneeUserIdAndPermissionSetFlagsId(
        assigneeUserId: Long,
        permissionSetFlagsId: Long,
    ): PermissionSetAssignment?

    /** Spec #837 — PS 자체 삭제 시 active/inactive 무관 전체 행 일괄 hard delete (cascade). */
    fun findAllByPermissionSetFlagsId(permissionSetFlagsId: Long): List<PermissionSetAssignment>
}
