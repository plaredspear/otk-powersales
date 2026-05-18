package com.otoki.powersales.admin.dto.response

import com.otoki.powersales.admin.sap.OutboundTriggerType
import java.time.LocalDateTime

// ========== Inbound ==========

data class SapInboundCatalogItemDto(
    val endpointPath: String,
    val koreanName: String,
    val requiredScope: String,
    val targetEntity: String,
    val controllerClass: String,
    val description: String,
)

data class SapInboundAuditRow(
    val id: Long,
    val eventType: String,
    val clientId: String?,
    val endpoint: String?,
    val httpMethod: String?,
    val clientIp: String,
    val scope: String?,
    val receivedCount: Int?,
    val previousCount: Int?,
    val reason: String?,
    val createdAt: LocalDateTime,
)

data class SapInboundAuditDetail(
    val id: Long,
    val eventType: String,
    val clientId: String?,
    val endpoint: String?,
    val httpMethod: String?,
    val clientIp: String,
    val scope: String?,
    val receivedCount: Int?,
    val previousCount: Int?,
    val reason: String?,
    val createdAt: LocalDateTime,
)

data class SapInboundAuditListResponse(
    val items: List<SapInboundAuditRow>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)

// ========== Outbound ==========

data class SapOutboundCatalogItemDto(
    val interfaceId: String,
    val koreanName: String,
    val triggerType: OutboundTriggerType,
    val senderClass: String,
    val description: String,
)

data class SapOutboundLogRow(
    val id: Long,
    val interfaceId: String,
    val endpointPath: String,
    val requestCount: Int,
    val httpStatus: Int?,
    val resultCode: String?,
    val resultMsg: String?,
    val attemptCount: Int,
    val durationMs: Long,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime,
)

data class SapOutboundLogDetail(
    val id: Long,
    val interfaceId: String,
    val endpointPath: String,
    val requestCount: Int,
    val httpStatus: Int?,
    val resultCode: String?,
    val resultMsg: String?,
    val attemptCount: Int,
    val durationMs: Long,
    val errorDetail: String?,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime,
)

data class SapOutboundLogListResponse(
    val items: List<SapOutboundLogRow>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)

data class SapOutboxPendingRow(
    val id: Long,
    val domainType: String,
    val aggregateId: Long,
    val interfaceId: String,
    val status: String,
    val retryCount: Int,
    val lastError: String?,
    val createdAt: LocalDateTime?,
    val sentAt: LocalDateTime?,
)

data class SapOutboxPendingListResponse(
    val items: List<SapOutboxPendingRow>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)
