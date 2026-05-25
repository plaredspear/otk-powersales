package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.GroupMember
import org.springframework.data.jpa.repository.JpaRepository

/**
 * SF GroupMember 적재 결과 lookup repository (spec #790).
 *
 * GroupMembershipEvaluator 가 PublicGroup (Group.Type=Regular) 멤버십 평가 시 본 repository 사용.
 */
interface GroupMemberRepository : JpaRepository<GroupMember, Long>, GroupMemberRepositoryCustom {

    /**
     * 본 Group 의 모든 멤버 일람 (User + Group).
     */
    fun findAllByGroupId(groupId: Long): List<GroupMember>
}
