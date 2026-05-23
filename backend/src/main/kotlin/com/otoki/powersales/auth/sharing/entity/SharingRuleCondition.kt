package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.salesforce.SFMeta
import com.otoki.powersales.common.salesforce.SFMetaSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SharingRule 의 Criteria-based 조건 1행 (spec #782 P1-B).
 *
 * `<sharingCriteriaRules><criteriaItems><field>...</field><operation>...</operation><value>...</value>`
 * 본문 1건 = 본 entity 1건. rule 안의 condition 적용 순서는 `conditionOrder` 로 박제.
 */
@Entity
@SFMeta(SFMetaSource.SHARING_RULES_XML, "criteriaItems")
@Table(name = "sharing_rule_condition")
class SharingRuleCondition(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharing_rule_condition_id")
    val id: Long = 0,

    @Column(name = "sharing_rule_id", nullable = false)
    var sharingRuleId: Long,

    @Column(name = "field", nullable = false, length = 80)
    var field: String,

    @Column(name = "operator", nullable = false, length = 20)
    var operator: String,

    // value 는 H2 / 일부 DB reserved keyword 라 condition_value 로 컬럼명 회피.
    @Column(name = "condition_value", columnDefinition = "TEXT")
    var value: String? = null,

    @Column(name = "condition_order", nullable = false)
    var conditionOrder: Int,

    @Column(name = "logic_connector", length = 10)
    var logicConnector: String? = null,
)
