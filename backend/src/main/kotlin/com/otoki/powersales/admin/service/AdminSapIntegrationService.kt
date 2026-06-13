package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminSapInboundAuditQuery
import com.otoki.powersales.admin.dto.request.AdminSapOutboundLogQuery
import com.otoki.powersales.admin.dto.response.SapInboundAuditDetail
import com.otoki.powersales.admin.dto.response.SapInboundAuditListResponse
import com.otoki.powersales.admin.dto.response.SapInboundAuditRow
import com.otoki.powersales.admin.dto.response.SapInboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundLogDetail
import com.otoki.powersales.admin.dto.response.SapOutboundLogListResponse
import com.otoki.powersales.admin.dto.response.SapOutboundLogRow
import com.otoki.powersales.admin.dto.response.SapOutboxPendingListResponse
import com.otoki.powersales.admin.dto.response.SapOutboxPendingRow
import com.otoki.powersales.admin.sap.SapInboundCatalog
import com.otoki.powersales.admin.sap.SapOutboundCatalog
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditRepository
import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.external.sap.outbound.repository.SapOutboundLogRepository
import com.otoki.powersales.external.sap.outbox.SapOutbox
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Admin SAP 연동 운영 모니터링 조회 서비스.
 *
 * - 카탈로그: `SapInboundCatalog` / `SapOutboundCatalog` 코드 SoT 그대로 반환
 * - 인바운드 audit / 아웃바운드 log: repository QueryDSL 동적 필터 후 DTO 변환
 * - 아웃박스 대기 큐: PENDING + RETRY 만 페이지 조회, `created_at ASC` (가장 오래 대기한 항목 우선)
 *
 * 모든 endpoint 는 [com.otoki.powersales.admin.security.AdminPermission.SAP_INTEGRATION_READ] 권한 필수
 * — `RolePermissionMatrix` 의 `SYSTEM_ADMIN` 에만 부여.
 */
@Service
@Transactional(readOnly = true)
class AdminSapIntegrationService(
    private val sapInboundAuditRepository: SapInboundAuditRepository,
    private val sapOutboundLogRepository: SapOutboundLogRepository,
    private val sapOutboxRepository: SapOutboxRepository,
) {

    fun inboundCatalog(): List<SapInboundCatalogItemDto> =
        SapInboundCatalog.ITEMS.map {
            SapInboundCatalogItemDto(
                endpointPath = it.endpointPath,
                koreanName = it.koreanName,
                requiredScope = it.requiredScope,
                targetEntity = it.targetEntity,
                controllerClass = it.controllerClass,
                description = it.description,
            )
        }

    fun searchInboundAudits(query: AdminSapInboundAuditQuery): SapInboundAuditListResponse {
        val page = (query.page - 1).coerceAtLeast(0)
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, size)

        val result = sapInboundAuditRepository.search(
            clientId = query.clientId,
            eventType = query.eventType,
            endpoint = query.endpoint,
            from = query.from,
            to = query.to,
            pageable = pageable,
        )

        return SapInboundAuditListResponse(
            items = result.content.map { it.toRow() },
            totalCount = result.totalElements,
            currentPage = query.page,
            pageSize = size,
        )
    }

    fun getInboundAuditDetail(id: Long): SapInboundAuditDetail {
        val audit = sapInboundAuditRepository.findById(id).orElseThrow {
            BusinessException(
                errorCode = "SAP_INBOUND_AUDIT_NOT_FOUND",
                message = "SAP 인바운드 audit 을 찾을 수 없습니다: $id",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        }
        return audit.toDetail()
    }

    fun outboundCatalog(): List<SapOutboundCatalogItemDto> =
        SapOutboundCatalog.ITEMS.map {
            SapOutboundCatalogItemDto(
                interfaceId = it.interfaceId,
                koreanName = it.koreanName,
                triggerType = it.triggerType,
                senderClass = it.senderClass,
                description = it.description,
            )
        }

    fun searchOutboundLogs(query: AdminSapOutboundLogQuery): SapOutboundLogListResponse {
        val page = (query.page - 1).coerceAtLeast(0)
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, size)

        val result = sapOutboundLogRepository.search(
            interfaceId = query.interfaceId,
            resultCode = query.resultCode,
            from = query.from,
            to = query.to,
            pageable = pageable,
        )

        return SapOutboundLogListResponse(
            items = result.content.map { it.toRow() },
            totalCount = result.totalElements,
            currentPage = query.page,
            pageSize = size,
        )
    }

    fun getOutboundLogDetail(id: Long): SapOutboundLogDetail {
        val log = sapOutboundLogRepository.findById(id).orElseThrow {
            BusinessException(
                errorCode = "SAP_OUTBOUND_LOG_NOT_FOUND",
                message = "SAP 아웃바운드 호출 이력을 찾을 수 없습니다: $id",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        }
        return log.toDetail()
    }

    fun searchOutboxPending(page: Int, size: Int): SapOutboxPendingListResponse {
        val zeroBasedPage = (page - 1).coerceAtLeast(0)
        val boundedSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(zeroBasedPage, boundedSize)

        val result = sapOutboxRepository.pagePendingOrRetry(pageable)

        return SapOutboxPendingListResponse(
            items = result.content.map { it.toPendingRow() },
            totalCount = result.totalElements,
            currentPage = page,
            pageSize = boundedSize,
        )
    }

    private fun SapInboundAudit.toRow(): SapInboundAuditRow = SapInboundAuditRow(
        id = id,
        eventType = eventType,
        clientId = clientId,
        endpoint = endpoint,
        httpMethod = httpMethod,
        clientIp = clientIp,
        scope = scope,
        receivedCount = receivedCount,
        previousCount = previousCount,
        reason = reason,
        createdAt = createdAt,
    )

    private fun SapInboundAudit.toDetail(): SapInboundAuditDetail = SapInboundAuditDetail(
        id = id,
        eventType = eventType,
        clientId = clientId,
        endpoint = endpoint,
        httpMethod = httpMethod,
        clientIp = clientIp,
        scope = scope,
        receivedCount = receivedCount,
        previousCount = previousCount,
        reason = reason,
        createdAt = createdAt,
    )

    private fun SapOutboundLog.toRow(): SapOutboundLogRow = SapOutboundLogRow(
        id = id,
        interfaceId = interfaceId,
        endpointPath = endpointPath,
        requestCount = requestCount,
        httpStatus = httpStatus,
        resultCode = resultCode,
        resultMsg = resultMsg,
        attemptCount = attemptCount,
        durationMs = durationMs,
        requestedAt = requestedAt,
        completedAt = completedAt,
    )

    private fun SapOutboundLog.toDetail(): SapOutboundLogDetail = SapOutboundLogDetail(
        id = id,
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
        completedAt = completedAt,
    )

    private fun SapOutbox.toPendingRow(): SapOutboxPendingRow = SapOutboxPendingRow(
        id = id,
        domainType = domainType,
        aggregateId = aggregateId,
        interfaceId = interfaceId,
        status = status,
        retryCount = retryCount,
        lastError = lastError,
        createdAt = createdAt,
        sentAt = sentAt,
    )

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
