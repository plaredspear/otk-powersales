package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminSapInboundAuditQuery
import com.otoki.powersales.admin.dto.request.AdminSapOutboundLogQuery
import com.otoki.powersales.admin.dto.response.SapInboundAuditDetail
import com.otoki.powersales.admin.dto.response.SapInboundAuditListResponse
import com.otoki.powersales.admin.dto.response.SapInboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundLogDetail
import com.otoki.powersales.admin.dto.response.SapOutboundLogListResponse
import com.otoki.powersales.admin.dto.response.SapOutboxPendingListResponse
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.admin.service.AdminSapIntegrationService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Admin SAP 연동 운영 모니터링 endpoint.
 *
 * - `/inbound/catalog`         9개 인바운드 endpoint 정적 카탈로그 (SapInboundCatalog SoT)
 * - `/inbound/audits`          페이지네이션 + 동적 필터 (clientId/eventType/endpoint/from/to)
 * - `/inbound/audits/{id}`     단건 상세
 * - `/outbound/catalog`        7개 아웃바운드 interface 정적 카탈로그 (SapOutboundCatalog SoT)
 * - `/outbound/logs`           페이지네이션 + 동적 필터 (interfaceId/resultCode/from/to)
 * - `/outbound/logs/{id}`      단건 상세 (errorDetail 포함)
 * - `/outbound/outbox-pending` PENDING + RETRY 큐 적체 모니터링
 *
 * 모든 endpoint 는 [AdminPermission.SAP_INTEGRATION_READ] 권한 필수 — `SYSTEM_ADMIN` 만 부여.
 */
@RestController
class AdminSapIntegrationController(
    private val adminSapIntegrationService: AdminSapIntegrationService,
) {

    @GetMapping("/api/v1/admin/sap-integration/inbound/catalog")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun getInboundCatalog(): ResponseEntity<ApiResponse<List<SapInboundCatalogItemDto>>> {
        return ResponseEntity.ok(ApiResponse.success(adminSapIntegrationService.inboundCatalog()))
    }

    @GetMapping("/api/v1/admin/sap-integration/inbound/audits")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun searchInboundAudits(
        @RequestParam(required = false) clientId: String?,
        @RequestParam(required = false) eventType: String?,
        @RequestParam(required = false) endpoint: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<SapInboundAuditListResponse>> {
        val response = adminSapIntegrationService.searchInboundAudits(
            AdminSapInboundAuditQuery(
                clientId = clientId,
                eventType = eventType,
                endpoint = endpoint,
                from = from,
                to = to,
                page = page,
                size = size,
            )
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/sap-integration/inbound/audits/{id}")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun getInboundAuditDetail(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SapInboundAuditDetail>> {
        return ResponseEntity.ok(ApiResponse.success(adminSapIntegrationService.getInboundAuditDetail(id)))
    }

    @GetMapping("/api/v1/admin/sap-integration/outbound/catalog")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun getOutboundCatalog(): ResponseEntity<ApiResponse<List<SapOutboundCatalogItemDto>>> {
        return ResponseEntity.ok(ApiResponse.success(adminSapIntegrationService.outboundCatalog()))
    }

    @GetMapping("/api/v1/admin/sap-integration/outbound/logs")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun searchOutboundLogs(
        @RequestParam(required = false) interfaceId: String?,
        @RequestParam(required = false) resultCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<SapOutboundLogListResponse>> {
        val response = adminSapIntegrationService.searchOutboundLogs(
            AdminSapOutboundLogQuery(
                interfaceId = interfaceId,
                resultCode = resultCode,
                from = from,
                to = to,
                page = page,
                size = size,
            )
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/sap-integration/outbound/logs/{id}")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun getOutboundLogDetail(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SapOutboundLogDetail>> {
        return ResponseEntity.ok(ApiResponse.success(adminSapIntegrationService.getOutboundLogDetail(id)))
    }

    @GetMapping("/api/v1/admin/sap-integration/outbound/outbox-pending")
    @RequiresPermission(AdminPermission.SAP_INTEGRATION_READ)
    fun getOutboxPending(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<SapOutboxPendingListResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminSapIntegrationService.searchOutboxPending(page, size)))
    }
}
