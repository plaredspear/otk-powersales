package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SharingRule 의 대상 (Role / Group / User) 1행 (spec #782 P1-B).
 *
 * `<sharedTo>` 본문 element 1건 = 본 entity 1건.
 *
 * target_sfid 는 polymorphic — `00E` prefix = UserRole / `00G` = Group / `005` = User.
 * Stage 2 fk substep 으로 prefix 분기 후 target_id 채움.
 */
@Entity
@Table(name = "sharing_rule_target")
class SharingRuleTarget(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharing_rule_target_id")
    val id: Long = 0,

    @Column(name = "sharing_rule_id", nullable = false)
    var sharingRuleId: Long,

    @Column(name = "target_type", nullable = false, length = 30)
    var targetType: String,

    @Column(name = "target_sfid", length = 18)
    var targetSfid: String? = null,

    @Column(name = "target_id")
    var targetId: Long? = null,
)
