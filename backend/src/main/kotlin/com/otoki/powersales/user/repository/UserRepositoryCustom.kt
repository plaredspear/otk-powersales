package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {

    /**
     * web admin User 관리 화면 목록 조회.
     *
     * - keyword 가 있으면 username / employee_code / name 부분 일치 (case-insensitive)
     * - isActive 가 null 이 아니면 정확 일치
     * - 정렬: name ASC
     */
    fun findUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User>

    /**
     * Spec #803 — Profile 상세의 부여 사용자 일람.
     *
     * - profileId 정확 일치
     * - keyword 가 있으면 employee_code / name 부분 일치 (case-insensitive)
     * - 정렬: name ASC
     */
    fun findUsersByProfileId(profileId: Long, keyword: String?, pageable: Pageable): Page<User>

    /**
     * Spec #803 — PermissionSet 상세의 부여 사용자 일람.
     *
     * - permission_set_assignment 의 active row 가 있는 user 만
     * - keyword 가 있으면 employee_code / name 부분 일치
     */
    fun findUsersByPermissionSetFlagsId(permissionSetFlagsId: Long, keyword: String?, pageable: Pageable): Page<User>
}
