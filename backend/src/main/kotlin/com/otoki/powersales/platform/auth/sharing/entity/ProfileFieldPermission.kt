package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFMeta
import com.otoki.powersales.platform.common.salesforce.SFMetaSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * Profile × Field FLS (spec #795).
 *
 * 운영 0건 (Profile 측 fieldPermissions 미사용 — PermissionSet 위임). XML 출처: `profiles/<Name>.profile-meta.xml`
 * 의 `<fieldPermissions>` element.
 */
@DomainName("프로파일 필드권한")
@Entity
@SFMeta(SFMetaSource.PROFILE_XML, "fieldPermissions")
@Table(name = "profile_field_permission")
class ProfileFieldPermission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("프로파일필드권한ID")
    @Column(name = "profile_field_permission_id")
    val id: Long = 0,

    @FieldName("프로파일ID")
    @Column(name = "profile_id")
    var profileId: Long? = null,

    @FieldName("프로파일명")
    @Column(name = "profile_name", nullable = false, length = 255)
    var profileName: String,

    @FieldName("SObject명")
    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @FieldName("필드명")
    @Column(name = "field_name", nullable = false, length = 80)
    var fieldName: String,

    @FieldName("조회가능여부")
    @Column(name = "readable", nullable = false)
    var readable: Boolean = false,

    @FieldName("수정가능여부")
    @Column(name = "editable", nullable = false)
    var editable: Boolean = false,
) : BaseEntity()
