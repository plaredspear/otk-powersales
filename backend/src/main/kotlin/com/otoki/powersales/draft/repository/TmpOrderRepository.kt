package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpOrder
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface TmpOrderRepository : JpaRepository<TmpOrder, Long> {

    fun findByEmployeeId(employeeId: Long): TmpOrder?

    /**
     * 동시성 정책 (#596 §2.5) — 사번당 1건 UPSERT 직렬화용 row lock.
     * 트랜잭션 내에서만 호출. 다른 트랜잭션은 락 해제까지 대기.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TmpOrder t WHERE t.employeeId = :employeeId")
    fun findByEmployeeIdForUpdate(employeeId: Long): TmpOrder?

    fun deleteByEmployeeId(employeeId: Long): Long
}
