package com.otoki.powersales.auth.web.repository

import com.otoki.powersales.auth.web.entity.ImpersonationLog
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 대행 로그인 감사 로그 리포지토리 (Spec #851).
 *
 * 적재(INSERT) + 종료 시 활성 row 조회 용도. 조회 API 는 본 spec 비범위 (Q5 옵션 1).
 */
interface ImpersonationLogRepository : JpaRepository<ImpersonationLog, Long> {

    /**
     * 종료 처리 대상 활성 row 조회 — (adminUserId, targetUserId) 쌍 중 ended_at 이 null 인 가장 최근 시작 row.
     *
     * 동일 쌍이 미종료로 다수 존재(과거 만료 세션)하면 최신 것만 반환 → 최신 것만 종료 처리.
     */
    fun findFirstByAdminUserIdAndTargetUserIdAndEndedAtIsNullOrderByStartedAtDesc(
        adminUserId: Long,
        targetUserId: Long,
    ): ImpersonationLog?
}
