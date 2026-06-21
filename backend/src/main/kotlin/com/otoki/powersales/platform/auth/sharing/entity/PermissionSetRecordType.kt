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
 * PermissionSet × RecordType visibility (spec #794).
 *
 * 운영 10건 (X1_1 / Marketing_ETC 등). XML 출처: `permissionsets/<Name>.permissionset-meta.xml`
 * 의 `<recordTypeVisibilities>` element.
 */
@DomainName("권한집합 레코드타입")
@Entity
@SFMeta(SFMetaSource.PERMISSION_SET_XML, "recordTypeVisibilities")
@Table(name = "permission_set_record_type")
class PermissionSetRecordType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("권한집합레코드타입ID")
    @Column(name = "permission_set_record_type_id")
    val id: Long = 0,

    @FieldName("권한집합ID")
    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    @FieldName("권한집합명")
    @Column(name = "permission_set_name", nullable = false, length = 255)
    var permissionSetName: String,

    @FieldName("레코드타입ID")
    @Column(name = "record_type_id")
    var recordTypeId: Long? = null,

    @FieldName("SObject명")
    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @FieldName("레코드타입개발자명")
    @Column(name = "record_type_developer_name", nullable = false, length = 80)
    var recordTypeDeveloperName: String,

    @FieldName("표시여부")
    @Column(name = "visible", nullable = false)
    var visible: Boolean = false,

    @FieldName("기본여부")
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
) : BaseEntity()
