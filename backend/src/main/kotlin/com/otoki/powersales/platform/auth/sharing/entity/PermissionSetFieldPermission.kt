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
    @FieldName("권한집합필드권한ID")
    @Column(name = "permission_set_field_permission_id")
    val id: Long = 0,

    @FieldName("권한집합ID")
    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    @FieldName("권한집합명")
    @Column(name = "permission_set_name", nullable = false, length = 255)
    var permissionSetName: String,

    @FieldName("객체명")
    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @FieldName("필드명")
    @Column(name = "field_name", nullable = false, length = 80)
    var fieldName: String,

    @FieldName("읽기권한")
    @Column(name = "readable", nullable = false)
    var readable: Boolean = false,

    @FieldName("편집권한")
    @Column(name = "editable", nullable = false)
    var editable: Boolean = false,
) : BaseEntity()
