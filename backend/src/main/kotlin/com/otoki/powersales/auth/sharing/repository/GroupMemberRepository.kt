package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.GroupMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * SF GroupMember 적재 결과 lookup repository (spec #790).
 *
 * GroupMembershipEvaluator 가 PublicGroup (Group.Type=Regular) 멤버십 평가 시 본 repository 사용.
 *
 * ## 메서드명 주의
 * Spring Data JPA 의 method-name parser 가 `UserOrGroup` 의 `Or` 를 키워드로 분해하지 못해
 * `user_or_group_id` 매핑이 실패한다 → `@Query` 명시로 우회.
 */
interface GroupMemberRepository : JpaRepository<GroupMember, Long> {

    /**
     * 본 Group 의 모든 멤버 일람 (User + Group).
     */
    fun findAllByGroupId(groupId: Long): List<GroupMember>

    /**
     * 본 User/Group 이 소속된 PublicGroup 일람 (역참조).
     *
     * @param targetId User.id 또는 Group.id
     * @param targetType "USER" 또는 "GROUP"
     */
    @Query(
        "SELECT gm FROM GroupMember gm WHERE gm.userOrGroupId = :targetId AND gm.userOrGroupType = :targetType",
    )
    fun findAllByMember(@Param("targetId") targetId: Long, @Param("targetType") targetType: String): List<GroupMember>
}
