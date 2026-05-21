package com.otoki.powersales.auth.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF Profile SObject 매핑 entity (Spec #780).
 *
 * SF 운영 Profile (573 필드) 중 운영 audit 의미가 있는 핵심 메타 8 필드만 보존.
 * Permissions* boolean 521+개는 미보존 — 신규 시스템은 AdminPermission enum 으로 권한 매트릭스 축약
 * (legacy-deviation.md 정책).
 *
 * 본 entity 는 read-only audit 용도. backend 권한 판정의 SoT 는 [com.otoki.powersales.user.entity.ProfileType]
 * enum 이며, [User.profileId] FK 는 SF 원본 Profile.Name lookup 의 trace 표시용.
 */
@Entity
@Table(name = "profile")
@SFObject("Profile")
class Profile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @SFField("UserType")
    @Column(name = "user_type", length = 40)
    var userType: String? = null,

    @SFField("Description")
    @Column(name = "description", length = 255)
    var description: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @Column(name = "created_by_id")
    var createdById: Long? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @Column(name = "last_modified_by_id")
    var lastModifiedById: Long? = null,

) : BaseEntity()
