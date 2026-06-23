package com.otoki.powersales.external.sap.outbound.service

import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.external.sap.outbound.repository.SapOutboundLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapOutboundLogService(
    private val sapOutboundLogRepository: SapOutboundLogRepository
) {

    /**
     * SAP outbound 송신 이력 1건을 `sap_outbound_log` 에 적재한다.
     *
     * `REQUIRES_NEW` — 호출 측(배치 트랜잭션 또는 [com.otoki.powersales.external.sap.outbound.SapOutboundResponseSink]
     * 가 동작하는 외부 호출 트랜잭션) 과 독립적으로 즉시 커밋한다. 호출 측이 이후 롤백/예외로 끝나도
     * 송신 이력은 보존되어야 하기 때문이다. 외부 호출 트랜잭션이 활성인 동안 두 번째 커넥션을 잠깐
     * 점유하므로 HikariCP 풀 크기는 2 이상이어야 한다 (단일 커넥션 풀이면 self-deadlock).
     */
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
