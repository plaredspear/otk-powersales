package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFMeta
import com.otoki.powersales.common.salesforce.SFMetaSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Profile × Field FLS (spec #795).
 *
 * 운영 0건 (Profile 측 fieldPermissions 미사용 — PermissionSet 위임). XML 출처: `profiles/<Name>.profile-meta.xml`
 * 의 `<fieldPermissions>` element.
 */
@Entity
@SFMeta(SFMetaSource.PROFILE_XML, "fieldPermissions")
@Table(name = "profile_field_permission")
class ProfileFieldPermission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_field_permission_id")
    val id: Long = 0,

    @Column(name = "profile_id")
    var profileId: Long? = null,

    @Column(name = "profile_name", nullable = false, length = 255)
    var profileName: String,

    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @Column(name = "field_name", nullable = false, length = 80)
    var fieldName: String,

    @Column(name = "readable", nullable = false)
    var readable: Boolean = false,

    @Column(name = "editable", nullable = false)
    var editable: Boolean = false,
) : BaseEntity()
