package com.otoki.powersales.sap.service

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.sap.dto.SapSyncResult
import com.otoki.powersales.sap.entity.SapSyncLog
import com.otoki.powersales.sap.repository.SapSyncLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class SapSyncLogService(
    private val sapSyncLogRepository: SapSyncLogRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun log(
        apiName: String,
        requestCount: Int,
        result: SapSyncResult,
        durationMs: Long,
        requestIp: String?,
        requestedAt: LocalDateTime
    ): SapSyncLog {
        val errorDetail = if (result.errors.isNotEmpty()) {
            objectMapper.writeValueAsString(result.errors)
        } else null

        val log = SapSyncLog(
            apiName = apiName,
            requestCount = requestCount,
            successCount = result.successCount,
            failCount = result.failCount,
            errorDetail = errorDetail,
            durationMs = durationMs,
            requestIp = requestIp,
            requestedAt = requestedAt,
            completedAt = LocalDateTime.now()
        )
        return sapSyncLogRepository.save(log)
    }
}
