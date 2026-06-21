package com.otoki.powersales.platform.auth.sharing.entity

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
 * SharingRule 의 Criteria-based 조건 1행 (spec #782 P1-B).
 *
 * `<sharingCriteriaRules><criteriaItems><field>...</field><operation>...</operation><value>...</value>`
 * 본문 1건 = 본 entity 1건. rule 안의 condition 적용 순서는 `conditionOrder` 로 박제.
 */
@DomainName("공유규칙조건")
@Entity
@SFMeta(SFMetaSource.SHARING_RULES_XML, "criteriaItems")
@Table(name = "sharing_rule_condition")
class SharingRuleCondition(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("공유규칙조건ID")
    @Column(name = "sharing_rule_condition_id")
    val id: Long = 0,

    @FieldName("공유규칙ID")
    @Column(name = "sharing_rule_id", nullable = false)
    var sharingRuleId: Long,

    @FieldName("공유규칙SObject명")
    @Column(name = "sharing_rule_s_object_name", nullable = false, length = 80)
    var sharingRuleSObjectName: String,

    @FieldName("필드")
    @Column(name = "field", nullable = false, length = 80)
    var field: String,

    @FieldName("연산자")
    @Column(name = "operator", nullable = false, length = 20)
    var operator: String,

    // value 는 H2 / 일부 DB reserved keyword 라 condition_value 로 컬럼명 회피.
    @FieldName("조건값")
    @Column(name = "condition_value", columnDefinition = "TEXT")
    var value: String? = null,

    @FieldName("조건순서")
    @Column(name = "condition_order", nullable = false)
    var conditionOrder: Int,

    @FieldName("논리연결자")
    @Column(name = "logic_connector", length = 255)
    var logicConnector: String? = null,
)
