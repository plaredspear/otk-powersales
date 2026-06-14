package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderRepository :
    JpaRepository<TmpOrder, Long>,
    TmpOrderRepositoryCustom {

    fun findByEmployeeId(employeeId: Long): TmpOrder?

    fun deleteByEmployeeId(employeeId: Long): Long
}
