package com.otoki.powersales.sap.outbound.service

import com.otoki.powersales.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.sap.outbound.repository.SapOutboundLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapOutboundLogService(
    private val sapOutboundLogRepository: SapOutboundLogRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        interfaceId: String,
        endpointPath: String,
        requestCount: Int,
        httpStatus: Int?,
        resultCode: String?,
        resultMsg: String?,
        attemptCount: Int,
        durationMs: Long,
        errorDetail: String?,
        requestedAt: LocalDateTime,
        completedAt: LocalDateTime
    ): SapOutboundLog {
        val entity = SapOutboundLog(
            interfaceId = interfaceId,
            endpointPath = endpointPath,
            requestCount = requestCount,
            httpStatus = httpStatus,
            resultCode = resultCode,
            resultMsg = resultMsg,
            attemptCount = attemptCount,
            durationMs = durationMs,
            errorDetail = errorDetail,
            requestedAt = requestedAt,
            completedAt = completedAt
        )
        return sapOutboundLogRepository.save(entity)
    }
}
