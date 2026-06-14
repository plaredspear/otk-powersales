package com.otoki.powersales.domain.sales.sfsync

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SF 거래처목표등록마스터 동기화 수동 실행 테스트 API (개발자 도구 — 외부 API 테스트).
 *
 * 주기 배치([com.otoki.powersales.platform.batch.SalesProgressRateMasterSyncBatch]) 와 동일한
 * SF fetch → ExternalKey 기준 upsert 경로를 즉시 1회 실행하고 upsert 통계를 반환한다.
 * 운영 배치를 기다리지 않고 SF→DB 동기화를 검증하기 위한 용도이며, 호출 즉시 실제 DB 에
 * upsert 가 반영된다. SF fetch 통신부가 아직 미구현(TODO)이라 현재는 no-op(fetched=0)으로 동작한다.
 * 다른 외부 API 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminSalesProgressRateMasterSyncTestController(
    private val syncService: SalesProgressRateMasterSyncService,
) {

    @PostMapping("/api/v1/admin/sales-progress-rate-master/sync/test")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(): ResponseEntity<ApiResponse<AdminSalesProgressRateMasterSyncTestResponse>> {
        val result = syncService.sync()
        return ResponseEntity.ok(
            ApiResponse.success(AdminSalesProgressRateMasterSyncTestResponse.from(result)),
        )
    }
}
