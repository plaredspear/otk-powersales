package com.otoki.powersales.domain.activity.claim.controller

import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimMasterSyncTestRequest
import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimMasterSyncTestResponse
import com.otoki.powersales.domain.activity.claim.service.AdminClaimMasterSyncTestService
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
 * SF `IF_SendClaimToPWS` 클레임 마스터 조회 + 갱신 API (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 클레임 마스터 조회. 요청 body `MOD_DT`(기준 일자) 로 SF Apex REST `/IF_SendClaimToPWS`
 * 로 POST 하면 SF 가 해당 일자 기준 변경 클레임 마스터 목록을 응답한다. 각 레코드의 `pwrskey`(=claim_id) 로
 * 신규 claim 을 찾아 조치/상담 6필드를 신규 데이터로 갱신하고, 갱신 집계 결과를 SF 응답 원형과 함께 반환한다.
 * claim-regist 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminClaimMasterSyncTestController(
    private val service: AdminClaimMasterSyncTestService,
) {

    @PostMapping("/api/v1/admin/claim-master-sync/test")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminClaimMasterSyncTestRequest,
    ): ResponseEntity<ApiResponse<AdminClaimMasterSyncTestResponse>> {
        val response = service.test(
            userId = principal.requireEmployeeId(),
            request = request,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
