package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionDryRunService
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SF 권한 cut-over 사전 검증 endpoint (spec #801 §4.1).
 *
 * 운영 admin 의 SF PermissionSet 부여로 검증 대상 endpoint 가 통과되는지 dry-run.
 * cut-over 작업 윈도우 전 의무 호출 — SYSTEM_ADMIN 만 접근 가능.
 */
@RestController
@RequestMapping("/api/v1/admin/sf-permission")
class SfPermissionDryRunController(
    private val dryRunService: SfPermissionDryRunService,
) {

    @PostMapping("/dry-run")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun runDryRun(
        @RequestBody request: SfPermissionDryRunRequest,
    ): ResponseEntity<ApiResponse<SfPermissionDryRunService.DryRunResult>> {
        val checks = request.checks.map {
            SfPermissionDryRunService.DryRunCheck(
                entity = it.entity,
                operation = it.operation,
                systemPermission = it.systemPermission,
            )
        }
        val result = dryRunService.dryRun(
            userIds = request.userIds.orEmpty().toSet(),
            employeeCodes = request.employeeCodes.orEmpty().toSet(),
            checks = checks,
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    data class SfPermissionDryRunRequest(
        val userIds: List<Long>? = null,
        val employeeCodes: List<String>? = null,
        val checks: List<DryRunCheckRequest>,
    )

    data class DryRunCheckRequest(
        val entity: String? = null,
        val operation: SfPermissionOperation,
        val systemPermission: SfSystemPermission? = null,
    )
}
