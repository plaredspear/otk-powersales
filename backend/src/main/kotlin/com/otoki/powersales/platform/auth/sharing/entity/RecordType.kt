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
import jakarta.persistence.UniqueConstraint

/**
 * SF RecordType 정의 (spec #794).
 *
 * XML 출처: `objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml`
 * Master RT 는 적재하지 않음 (Q4 옵션 1) — `record_type_id IS NULL` 이 곧 Master 의미.
 */
@Entity
@SFMeta(SFMetaSource.RECORD_TYPE_XML)
@Table(
    name = "record_type",
    uniqueConstraints = [UniqueConstraint(name = "record_type_natural_key_unique", columnNames = ["sobject_name", "developer_name"])],
)
class RecordType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_type_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    var sfid: String? = null,

    @Column(name = "sobject_name", nullable = false, length = 80)
    var sObjectName: String,

    @Column(name = "developer_name", nullable = false, length = 80)
    var developerName: String,

    @Column(name = "label", nullable = false, length = 255)
    var label: String,

    @Column(name = "description", length = 1024)
    var description: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity()
