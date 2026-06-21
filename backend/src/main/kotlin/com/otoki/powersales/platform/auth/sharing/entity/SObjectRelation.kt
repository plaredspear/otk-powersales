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
 * SF master-detail relationship 정규화 메타 (spec #791 Q2 옵션 1).
 *
 * ControlledByParent SObject 의 parent SObject 추론 출처. SF describe API 의 `childRelationships`
 * 와 정합. extract-sharing-meta.sh 의 master-detail 추출 결과 적재.
 */
@DomainName("SObject관계메타")
@Entity
@SFMeta(SFMetaSource.DESCRIBE_API, "childRelationships")
@Table(
    name = "sobject_relation",
    uniqueConstraints = [UniqueConstraint(name = "sobject_relation_unique", columnNames = ["child_sobject_name", "relation_field_name"])],
)
class SObjectRelation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("SObject관계ID")
    @Column(name = "sobject_relation_id")
    val id: Long = 0,

    @FieldName("자식SObject명")
    @Column(name = "child_sobject_name", nullable = false, length = 80)
    var childSObjectName: String,

    @FieldName("부모SObject명")
    @Column(name = "parent_sobject_name", nullable = false, length = 80)
    var parentSObjectName: String,

    @FieldName("관계필드명")
    @Column(name = "relation_field_name", nullable = false, length = 80)
    var relationFieldName: String,

    @FieldName("마스터디테일여부")
    @Column(name = "is_master_detail", nullable = false)
    var isMasterDetail: Boolean = true,
) : BaseEntity()
