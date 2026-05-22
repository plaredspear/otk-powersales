package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF GroupMember mirror (spec #790).
 *
 * SF `Group.Type = 'Regular'` (PublicGroup) 의 멤버십. `UserOrGroupId` 가 005 (User) / 00G (Group) 의 polymorphic.
 *
 * ## 적재 흐름
 * 1. Stage 1: `extract-csv.sh` 의 SOQL 출처 — `group_members.csv` 의 Id / GroupId / UserOrGroupId.
 * 2. Stage 2 fk resolve: prefix 분기 후 group_id + user_or_group_id + user_or_group_type 채움.
 *
 * ## evaluator 인계
 * `GroupMembershipEvaluator` 가 본 테이블을 lookup 하여 SF PublicGroup 멤버십 평가를 흡수 — 본 spec 시점에는
 * 운영 0건 + sharingRule `<group>` 인용 1건만이라 evaluator 보강은 별도 진행.
 *
 * `@SFObject` 어노테이션 미부착 — verify-metadata.main.kts 면제 (운영 데이터 mirror).
 */
@Entity
@Table(name = "group_member")
class GroupMember(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_id")
    val id: Long = 0,

    @Column(name = "sfid", nullable = false, length = 18, unique = true)
    var sfid: String,

    @Column(name = "group_sfid", nullable = false, length = 18)
    var groupSfid: String,

    @Column(name = "group_id")
    var groupId: Long? = null,

    @Column(name = "user_or_group_sfid", nullable = false, length = 18)
    var userOrGroupSfid: String,

    @Column(name = "user_or_group_id")
    var userOrGroupId: Long? = null,

    @Column(name = "user_or_group_type", length = 10)
    var userOrGroupType: String? = null,
) : BaseEntity()
