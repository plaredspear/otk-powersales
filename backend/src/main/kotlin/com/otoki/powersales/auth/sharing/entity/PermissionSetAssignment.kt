package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * User ↔ PermissionSet 매핑 (spec #782 P1-B).
 *
 * 기존 `sf_permission_set_assignment_raw` (#764) 는 cut-over 1회성 staging — 본 entity 는
 * 운영 시점 PermissionSetEvaluator 가 매번 read 하는 정규 entity.
 */
@Entity
@Table(name = "permission_set_assignment")
class PermissionSetAssignment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_assignment_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @Column(name = "assignee_user_sfid", nullable = false, length = 18)
    var assigneeUserSfid: String,

    @Column(name = "assignee_user_id")
    var assigneeUserId: Long? = null,

    @Column(name = "permission_set_flags_id", nullable = false)
    var permissionSetFlagsId: Long,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "assigned_at")
    var assignedAt: LocalDateTime? = null,
)
