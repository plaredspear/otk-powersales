package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF SharingRule meta 본문 1행 (spec #782 P1-B).
 *
 * sharingRule 의 `<sharingCriteriaRules>` (rule_type=CRITERIA) 또는 `<sharingOwnerRules>` (rule_type=OWNER)
 * 본문을 정책 row 로 import 한 결과. 본 entity 1건 = SF rule 1건.
 *
 * 조건 (sharing_rule_condition) + 대상 (sharing_rule_target) 은 별도 테이블.
 */
@Entity
@Table(name = "sharing_rule")
class SharingRule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharing_rule_id")
    val id: Long = 0,

    @Column(name = "developer_name", nullable = false, length = 80, unique = true)
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
