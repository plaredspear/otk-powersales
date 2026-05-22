package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSetAssignment
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetAssignmentRepository : JpaRepository<PermissionSetAssignment, Long> {
    fun findAllByAssigneeUserIdAndIsActiveTrue(assigneeUserId: Long): List<PermissionSetAssignment>
}
