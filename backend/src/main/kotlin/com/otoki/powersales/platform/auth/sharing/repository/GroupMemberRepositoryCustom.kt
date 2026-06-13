package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.GroupMember

interface GroupMemberRepositoryCustom {

    /**
     * 본 User/Group 이 소속된 PublicGroup 일람 (역참조).
     *
     * Spring Data JPA 의 method-name parser 가 `UserOrGroup` 의 `Or` 를 키워드로 분해하여
     * `user_or_group_id` 매핑이 실패 → QueryDSL Custom Impl 로 우회.
     *
     * @param targetId User.id 또는 Group.id
     * @param targetType "USER" 또는 "GROUP"
     */
    fun findAllByMember(targetId: Long, targetType: String): List<GroupMember>
}
