package com.otoki.powersales.external.common.outboundlog.service

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import com.otoki.powersales.external.common.outboundlog.repository.ExternalApiLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 외부 HTTP outbound 호출 공통 로그 적재 서비스.
 *
 * [Propagation.REQUIRES_NEW] 로 호출자(도메인) 트랜잭션과 분리한다 — 로그 적재가 실패하거나
 * 도메인 트랜잭션이 롤백되어도 관측 로그는 독립적으로 남도록 한다.
 */
@Service
class ExternalApiLogService(
    private val externalApiLogRepository: ExternalApiLogRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        targetSystem: String,
        endpointKey: String?,
        httpMethod: String,
        uri: String,
        httpStatus: Int?,
        success: Boolean,
        durationMs: Long,
        errorDetail: String?,
        requestedAt: LocalDateTime,
        completedAt: LocalDateTime
    ): ExternalApiLog {
        val entity = ExternalApiLog(
            targetSystem = targetSystem,
            endpointKey = endpointKey,
            httpMethod = httpMethod,
            uri = uri.take(MAX_URI_LENGTH),
            httpStatus = httpStatus,
            success = success,
            durationMs = durationMs,
            errorDetail = errorDetail?.take(MAX_ERROR_DETAIL_LENGTH),
            requestedAt = requestedAt,
            completedAt = completedAt
        )
        return externalApiLogRepository.save(entity)
    }

    companion object {
        private const val MAX_URI_LENGTH = 1000
        private const val MAX_ERROR_DETAIL_LENGTH = 4000
    }
}
