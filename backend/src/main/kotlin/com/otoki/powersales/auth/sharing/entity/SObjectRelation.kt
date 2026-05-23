package com.otoki.powersales.auth.sharing.entity

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
 * SF master-detail relationship 정규화 메타 (spec #791 Q2 옵션 1).
 *
 * ControlledByParent SObject 의 parent SObject 추론 출처. SF describe API 의 `childRelationships`
 * 와 정합. extract-sharing-meta.sh 의 master-detail 추출 결과 적재.
 */
@Entity
@SFMeta(SFMetaSource.DESCRIBE_API, "childRelationships")
@Table(
    name = "sobject_relation",
    uniqueConstraints = [UniqueConstraint(name = "sobject_relation_unique", columnNames = ["child_sobject_name", "relation_field_name"])],
)
class SObjectRelation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sobject_relation_id")
    val id: Long = 0,

    @Column(name = "child_sobject_name", nullable = false, length = 80)
    var childSObjectName: String,

    @Column(name = "parent_sobject_name", nullable = false, length = 80)
    var parentSObjectName: String,

    @Column(name = "relation_field_name", nullable = false, length = 80)
    var relationFieldName: String,

    @Column(name = "is_master_detail", nullable = false)
    var isMasterDetail: Boolean = true,
) : BaseEntity()
