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

/**
 * PermissionSet × Field FLS (spec #795).
 *
 * 운영 26 PermissionSet. XML 출처: `permissionsets/<Name>.permissionset-meta.xml` 의 `<fieldPermissions>` element.
 */
@DomainName("권한집합 필드권한")
@Entity
@SFMeta(SFMetaSource.PERMISSION_SET_XML, "fieldPermissions")
@Table(name = "permission_set_field_permission")
class PermissionSetFieldPermission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_field_permission_id")
    val id: Long = 0,

    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    @Column(name = "permission_set_name", nullable = false, length = 255)
    var permissionSetName: String,

    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @Column(name = "field_name", nullable = false, length = 80)
    var fieldName: String,

    @Column(name = "readable", nullable = false)
    var readable: Boolean = false,

    @Column(name = "editable", nullable = false)
    var editable: Boolean = false,
) : BaseEntity()
