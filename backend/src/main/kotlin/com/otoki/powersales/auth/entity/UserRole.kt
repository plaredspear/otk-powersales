package com.otoki.powersales.auth.entity

import com.otoki.powersales.auth.sharing.listener.UserRoleEntityListener
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * SF UserRole SObject 매핑 entity (Spec #780).
 *
 * SF 운영 UserRole (16 필드) 중 운영 audit 의미가 있는 7 필드만 보존. Portal / Forecast /
 * Opportunity·Case·Contact Access picklist 9 필드는 운영 무관으로 미보존.
 *
 * 동일 패키지의 [UserRoleEnum] 과 책임 분리 — [UserRoleEnum] 은 backend 권한 판정의 SoT
 * (9종 운영 역할 + ALL_BRANCHES / BRANCH_SCOPE / ALLOWED_FOR_ADMIN_LOGIN set) 이며,
 * 본 entity 는 SF 원본 UserRole row 의 read-only audit lookup 테이블.
 *
 * BaseEntity 미상속 — SF UserRole 은 `CreatedDate` / `CreatedById` 필드 자체가 부재.
 * `LastModifiedDate` / `LastModifiedById` 만 audit 컬럼으로 보유.
 */
@Entity
@Table(name = "user_role")
@SFObject("UserRole")
@EntityListeners(UserRoleEntityListener::class)
class UserRole(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 80)
    var name: String,

    @SFField("DeveloperName")
    @Column(name = "developer_name", length = 80)
    var developerName: String? = null,

    @SFField("RollupDescription")
    @Column(name = "rollup_description", length = 80)
    var rollupDescription: String? = null,

    @SFField("ParentRoleId")
    @Column(name = "parent_user_role_sfid", length = 18)
    var parentUserRoleSfid: String? = null,

    @Column(name = "parent_user_role_id")
    var parentUserRoleId: Long? = null,

    @SFField("LastModifiedDate")
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @Column(name = "last_modified_by_id")
    var lastModifiedById: Long? = null,
)
