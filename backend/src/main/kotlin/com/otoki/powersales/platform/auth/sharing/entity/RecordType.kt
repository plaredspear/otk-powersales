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
import jakarta.persistence.UniqueConstraint
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * SF RecordType 정의 (spec #794).
 *
 * XML 출처: `objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml`
 * Master RT 는 적재하지 않음 (Q4 옵션 1) — `record_type_id IS NULL` 이 곧 Master 의미.
 */
@DomainName("레코드타입")
@Entity
@SFMeta(SFMetaSource.RECORD_TYPE_XML)
@Table(
    name = "record_type",
    uniqueConstraints = [UniqueConstraint(name = "record_type_natural_key_unique", columnNames = ["sobject_name", "developer_name"])],
)
class RecordType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("레코드타입ID")
    @Column(name = "record_type_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    var sfid: String? = null,

    @FieldName("SObject명")
    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @FieldName("개발자명")
    @Column(name = "developer_name", nullable = false, length = 80)
    var developerName: String,

    @FieldName("라벨")
    @Column(name = "label", nullable = false, length = 255)
    var label: String,

    @FieldName("행사대체제품")
    @Column(name = "description", length = 1024)
    var description: String? = null,

    @FieldName("활성여부")
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity()
