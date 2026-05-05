package com.otoki.powersales.sap.outbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SapOutboxRepository : JpaRepository<SapOutbox, Long> {

    @Query(
        "SELECT s FROM SapOutbox s " +
            "WHERE s.status IN ('PENDING', 'RETRY') " +
            "ORDER BY s.createdAt ASC"
    )
    fun findPendingOrRetry(pageable: Pageable): List<SapOutbox>
}
