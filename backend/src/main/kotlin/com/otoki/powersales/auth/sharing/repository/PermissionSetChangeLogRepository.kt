package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetChangeLogRepository : JpaRepository<PermissionSetChangeLog, Long> {

    /**
     * 특정 PS 의 변경 이력 페이지네이션 조회 (시간순 desc 정렬은 호출처 Pageable 로 지정).
     *
     * 주의: PS 삭제 후 row 의 [PermissionSetChangeLog.permissionSetId] 는 NULL 로 set 되어
     * 본 메서드로는 조회 불가. 삭제된 PS 의 audit 이력은 별도 endpoint (전체 조회) 가 필요하면 추가.
     */
    fun findByPermissionSetId(permissionSetId: Long, pageable: Pageable): Page<PermissionSetChangeLog>
}
