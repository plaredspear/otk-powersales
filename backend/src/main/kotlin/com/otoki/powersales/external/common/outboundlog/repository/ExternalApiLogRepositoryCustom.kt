package com.otoki.powersales.external.common.outboundlog.repository

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface ExternalApiLogRepositoryCustom {

    fun search(
        targetSystem: String?,
        endpointKey: String?,
        success: Boolean?,
        httpMethod: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<ExternalApiLog>
}
