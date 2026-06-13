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
 * PermissionSet × RecordType visibility (spec #794).
 *
 * 운영 10건 (X1_1 / Marketing_ETC 등). XML 출처: `permissionsets/<Name>.permissionset-meta.xml`
 * 의 `<recordTypeVisibilities>` element.
 */
@Entity
@SFMeta(SFMetaSource.PERMISSION_SET_XML, "recordTypeVisibilities")
@Table(name = "permission_set_record_type")
class PermissionSetRecordType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_record_type_id")
    val id: Long = 0,

    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    @Column(name = "permission_set_name", nullable = false, length = 255)
    var permissionSetName: String,

    @Column(name = "record_type_id")
    var recordTypeId: Long? = null,

    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @Column(name = "record_type_developer_name", nullable = false, length = 80)
    var recordTypeDeveloperName: String,

    @Column(name = "visible", nullable = false)
    var visible: Boolean = false,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
) : BaseEntity()
