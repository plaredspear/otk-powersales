package com.otoki.powersales.domain.sales.sfsync

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 조회 테스트 API (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 거래처 목표 마스터 조회. 요청 body `MOD_DT`(기준 일자) 로 SF Apex REST
 * `/IF_salesprogresssend` 로 POST 하면 SF 가 해당 일자 기준 변경 거래처목표등록마스터 목록을 응답한다.
 * SF 응답 원형을 그대로 반환하며, 요청 `save=true` 면 주기 sync 와 동일 경로(ExternalKey upsert)로
 * 신규 DB 에 저장한 통계도 함께 담는다 (`save=false` 기본 — 조회 전용, DB 변경 없음).
 * 다른 외부 API 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminSalesProgressRateMasterSyncTestController(
    private val service: AdminSalesProgressRateMasterSyncTestService,
) {

    @PostMapping("/api/v1/admin/sales-progress-rate-master/sync/test")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminSalesProgressRateMasterSyncTestRequest,
    ): ResponseEntity<ApiResponse<AdminSalesProgressRateMasterSyncTestResponse>> {
        val response = service.test(
            userId = principal.requireEmployeeId(),
            request = request,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
