package com.otoki.powersales.sap.auth.audit

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SapInboundAuditRepository : JpaRepository<SapInboundAudit, Long> {

    @Query(
        """
        SELECT a FROM SapInboundAudit a
        WHERE a.endpoint = :endpoint
          AND a.clientId = :clientId
          AND a.eventType = :eventType
        ORDER BY a.createdAt DESC
        """
    )
    fun findLatestByEndpointAndClientAndEvent(
        @Param("endpoint") endpoint: String,
        @Param("clientId") clientId: String,
        @Param("eventType") eventType: String,
        pageable: PageRequest
    ): List<SapInboundAudit>
}
