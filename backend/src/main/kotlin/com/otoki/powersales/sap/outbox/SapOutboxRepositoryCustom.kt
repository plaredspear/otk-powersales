package com.otoki.powersales.sap.outbox

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SapOutboxRepositoryCustom {

    fun findPendingOrRetry(pageable: Pageable): List<SapOutbox>

    fun pagePendingOrRetry(pageable: Pageable): Page<SapOutbox>
}
