package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.salesforce.SFObject
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
 *
 * 적재 출처: extract-csv.sh `PSA_SOQL` — `SELECT Id, AssigneeId, PermissionSetId, IsActive, CreatedDate
 *   FROM PermissionSetAssignment WHERE Assignee.IsActive = TRUE AND PermissionSet.IsCustom = TRUE`.
 */
@Entity
@SFObject("PermissionSetAssignment")
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

    // spec #798 — Stage1 적재 시점 sfid 박제용. Stage2 fk substep 이 permission_set_flags 로 lookup
    @Column(name = "permission_set_sfid", length = 18)
    var permissionSetSfid: String? = null,

    @Column(name = "permission_set_flags_id")
    var permissionSetFlagsId: Long? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "assigned_at")
    var assignedAt: LocalDateTime? = null,
)
