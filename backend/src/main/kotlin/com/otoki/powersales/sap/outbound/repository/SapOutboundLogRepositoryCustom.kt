package com.otoki.powersales.sap.outbound.repository

import com.otoki.powersales.sap.outbound.entity.SapOutboundLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface SapOutboundLogRepositoryCustom {

    fun search(
        interfaceId: String?,
        resultCode: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<SapOutboundLog>
}
