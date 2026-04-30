package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long> {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>
}
