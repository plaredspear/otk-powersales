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

/**
 * SF SharingRule meta 본문 1행 (spec #782 P1-B).
 *
 * sharingRule 의 `<sharingCriteriaRules>` (rule_type=CRITERIA) 또는 `<sharingOwnerRules>` (rule_type=OWNER)
 * 본문을 정책 row 로 import 한 결과. 본 entity 1건 = SF rule 1건.
 *
 * 조건 (sharing_rule_condition) + 대상 (sharing_rule_target) 은 별도 테이블.
 */
@Entity
@SFMeta(SFMetaSource.SHARING_RULES_XML, "sharingCriteriaRules")
@SFMeta(SFMetaSource.SHARING_RULES_XML, "sharingOwnerRules")
@Table(
    name = "sharing_rule",
    uniqueConstraints = [
        // SF SharingRule 의 자연 키는 (sObjectName, developerName) 복합 — V206 으로 단일 unique 에서 전환.
        // 동일 fullName 이 여러 sObject 의 sharingRules-meta.xml 에 동시 정의되는 케이스 (예: X5452) 대응.
        UniqueConstraint(
            name = "idx_sharing_rule_s_object_developer_name_unique",
            columnNames = ["s_object_name", "developer_name"],
        ),
    ],
)
class SharingRule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharing_rule_id")
    val id: Long = 0,

    @Column(name = "developer_name", nullable = false, length = 80)
    var developerName: String,

    @Column(name = "s_object_name", nullable = false, length = 80)
    var sObjectName: String,

    @Column(name = "rule_type", nullable = false, length = 20)
    var ruleType: String,

    @Column(name = "label", length = 255)
    var label: String? = null,

    @Column(name = "access_level", nullable = false, length = 10)
    var accessLevel: String,

    @Column(name = "include_owned_by_all", nullable = false)
    var includeOwnedByAll: Boolean = false,
) : BaseEntity()
