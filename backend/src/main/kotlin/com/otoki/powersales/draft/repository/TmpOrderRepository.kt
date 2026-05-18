package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderRepository :
    JpaRepository<TmpOrder, Long>,
    TmpOrderRepositoryCustom {

    fun findByEmployeeId(employeeId: Long): TmpOrder?

    fun deleteByEmployeeId(employeeId: Long): Long
}
