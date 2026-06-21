package com.otoki.powersales.platform.auth.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * SF UserRole SObject 매핑 entity (Spec #780).
 *
 * SF 운영 UserRole (16 필드) 중 운영 audit 의미가 있는 7 필드만 보존. Portal / Forecast /
 * Opportunity·Case·Contact Access picklist 9 필드는 운영 무관으로 미보존.
 *
 * SF Role Hierarchy 의 조직 계층 lookup 테이블. backend 권한 판정의 SoT 는 Profile + ProfileFlags +
 * PermissionSet (spec #801 SF 권한 모델). 본 entity 는 SF 원본 UserRole row 의 read-only audit.
 *
 * BaseEntity 미상속 — SF UserRole 은 `CreatedDate` / `CreatedById` 필드 자체가 부재.
 * `LastModifiedDate` / `LastModifiedById` 만 audit 컬럼으로 보유.
 */
@DomainName("사용자역할")
@Entity
@Table(name = "user_role")
@SFObject("UserRole")
class UserRole(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("사용자역할ID")
    @Column(name = "user_role_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", nullable = false, length = 80)
    var name: String,

    @SFField("DeveloperName")
    @FieldName("개발자명")
    @Column(name = "developer_name", length = 80)
    var developerName: String? = null,

    @SFField("RollupDescription")
    @FieldName("롤업설명")
    @Column(name = "rollup_description", length = 80)
    var rollupDescription: String? = null,

    @SFField("ParentRoleId")
    @Column(name = "parent_user_role_sfid", length = 18)
    var parentUserRoleSfid: String? = null,

    @FieldName("상위사용자역할ID")
    @Column(name = "parent_user_role_id")
    var parentUserRoleId: Long? = null,

    @SFField("LastModifiedDate")
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @FieldName("최종수정자ID")
    @Column(name = "last_modified_by_id")
    var lastModifiedById: Long? = null,
)
