package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long>, DailySalesHistoryRepositoryCustom {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>
}
