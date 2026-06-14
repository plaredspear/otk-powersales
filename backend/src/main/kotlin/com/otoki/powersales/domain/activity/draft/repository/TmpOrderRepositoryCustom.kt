package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpOrder

interface TmpOrderRepositoryCustom {

    /**
     * 동시성 정책 (#596 §2.5) — 사번당 1건 UPSERT 직렬화용 row lock.
     * 트랜잭션 내에서만 호출. 다른 트랜잭션은 락 해제까지 대기.
     */
    fun findByEmployeeIdForUpdate(employeeId: Long): TmpOrder?
}
