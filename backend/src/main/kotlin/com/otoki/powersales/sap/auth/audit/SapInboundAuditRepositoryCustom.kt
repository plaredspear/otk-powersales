package com.otoki.powersales.sap.auth.audit

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface SapInboundAuditRepositoryCustom {

    fun search(
        clientId: String?,
        eventType: String?,
        endpoint: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<SapInboundAudit>
}
