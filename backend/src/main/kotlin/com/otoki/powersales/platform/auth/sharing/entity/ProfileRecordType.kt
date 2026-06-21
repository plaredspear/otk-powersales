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
 * Profile × RecordType visibility (spec #794).
 *
 * 운영 0건 (Profile 의 RT visibility 미사용 — PermissionSet 위임). XML 출처: `profiles/<Name>.profile-meta.xml`
 * 의 `<recordTypeVisibilities>` element.
 */
@DomainName("프로파일 레코드타입")
@Entity
@SFMeta(SFMetaSource.PROFILE_XML, "recordTypeVisibilities")
@Table(name = "profile_record_type")
class ProfileRecordType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("프로파일레코드타입ID")
    @Column(name = "profile_record_type_id")
    val id: Long = 0,

    @FieldName("프로파일ID")
    @Column(name = "profile_id")
    var profileId: Long? = null,

    @FieldName("프로파일명")
    @Column(name = "profile_name", nullable = false, length = 255)
    var profileName: String,

    @FieldName("레코드타입ID")
    @Column(name = "record_type_id")
    var recordTypeId: Long? = null,

    @FieldName("객체명")
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
