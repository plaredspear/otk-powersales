package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminExternalApiLogQuery
import com.otoki.powersales.admin.dto.response.ExternalApiLogDetail
import com.otoki.powersales.admin.dto.response.ExternalApiLogListResponse
import com.otoki.powersales.admin.service.AdminExternalApiLogService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Admin 외부 API 호출 이력 조회 API (개발자 도구 — 외부 API 테스트 > 호출 이력).
 *
 * `external_api_log` (SAP/SF/Naver outbound HTTP 호출 공통 로그) 를 조회한다.
 * - `/logs`         페이지네이션 + 동적 필터 (targetSystem / endpointKey / success / httpMethod / from / to)
 * - `/logs/{id}`    단건 상세 (errorDetail 포함)
 * - `/log-keys`     필터 셀렉터용 endpoint key 목록
 *
 * SAP 연동 페이지의 outbound 로그 조회와 동일하게 SYSTEM(VIEW_ALL_DATA) 권한 필요.
 */
@RestController
class AdminExternalApiLogController(
    private val adminExternalApiLogService: AdminExternalApiLogService,
) {

    @GetMapping("/api/v1/admin/external-api/logs")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun searchLogs(
        @RequestParam(required = false) targetSystem: String?,
        @RequestParam(required = false) endpointKey: String?,
        @RequestParam(required = false) success: Boolean?,
        @RequestParam(required = false) httpMethod: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<ExternalApiLogListResponse>> {
        val response = adminExternalApiLogService.search(
            AdminExternalApiLogQuery(
                targetSystem = targetSystem,
                endpointKey = endpointKey,
                success = success,
                httpMethod = httpMethod,
                from = from,
                to = to,
                page = page,
                size = size,
            )
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/external-api/logs/{id}")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getLogDetail(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ExternalApiLogDetail>> {
        return ResponseEntity.ok(ApiResponse.success(adminExternalApiLogService.getDetail(id)))
    }

    @GetMapping("/api/v1/admin/external-api/log-keys")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getLogKeys(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(adminExternalApiLogService.endpointKeys()))
    }
}
