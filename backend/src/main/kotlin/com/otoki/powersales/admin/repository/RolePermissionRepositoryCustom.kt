package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.RolePermission

interface RolePermissionRepositoryCustom {

    fun findByRoleName(roleName: String): List<RolePermission>

    /**
     * 단일 JPQL `DELETE` 로 bulk 삭제 (영속성 컨텍스트 select-then-delete 회피).
     * 호출 컨텍스트의 트랜잭션 필요. persistence context 의 캐시된 row 는 자동 제거되지 않으므로
     * 본 호출 이후 동일 트랜잭션에서 이전 row 를 참조하지 않아야 한다.
     */
    fun deleteByRoleName(roleName: String): Long
}
