package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long> {

    fun findByExternalKey(externalKey: String): DailySalesHistory?
}
