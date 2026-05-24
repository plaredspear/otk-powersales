package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
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
 *
 * Spec #804 에서 BaseEntity 상속 전환 + createdById/updatedById audit FK 추가.
 * partial UNIQUE 인덱스 (V187) — 동일 (assignee_user_id, permission_set_flags_id) active row 1개 invariant.
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

    // V202 — sfid 는 SF 데이터 마이그레이션 보조 필드이며 service 로직 활용 금지 정책에 따라 nullable.
    // Stage1 적재분은 SF AssigneeId 박힘. web admin runtime 부여분은 NULL.
    @Column(name = "assignee_user_sfid", length = 18)
    var assigneeUserSfid: String? = null,

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

    /** Spec #804 — web admin 부여 시 부여자 User FK. cut-over Stage1 적재분은 null. */
    @Column(name = "created_by_id")
    var createdById: Long? = null,

    /** Spec #804 — web admin 회수/재부여 시 작업자 User FK. */
    @Column(name = "updated_by_id")
    var updatedById: Long? = null,

) : BaseEntity()
