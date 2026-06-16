package com.otoki.powersales.domain.org.employee.sfsync

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
 * SF `IF_SendStaffReviewToPWS` 사원평가 마스터 조회 테스트 API (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 사원평가(StaffReview) 마스터 조회. 요청 body `MOD_DT`(기준 일자) 로 SF Apex REST
 * `/IF_SendStaffReviewToPWS` 로 POST 하면 SF 가 해당 일자(수정일 기준) 변경 사원평가 마스터 목록을 응답한다.
 * 신규 DB 저장 없이 SF 응답 원형만 반환한다 (클레임/거래처목표 마스터 조회 테스트와 동일 성격).
 * 다른 외부 API 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminStaffReviewSyncTestController(
    private val service: AdminStaffReviewSyncTestService,
) {

    @PostMapping("/api/v1/admin/staff-review/sync/test")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminStaffReviewSyncTestRequest,
    ): ResponseEntity<ApiResponse<AdminStaffReviewSyncTestResponse>> {
        val response = service.test(
            userId = principal.requireEmployeeId(),
            request = request,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
