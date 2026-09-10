package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.promotion.service.PPTHistoryMasterIdBackfillService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 — 전문행사조 이력의 원인 마스터 FK(`master_id`) 백필 endpoint.
 *
 * SF 이관 이력에는 마스터 참조가 없어 FK 가 비어 있고, 그대로 두면 마스터 삭제 / 핵심필드 수정 가드가
 * 이관분 마스터를 보호하지 못한다 ([PPTHistoryMasterIdBackfillService] KDoc 참조).
 *
 * 권한: 운영 데이터 변경이므로 execute 는 [SfSystemPermission.MODIFY_ALL_DATA] (SYSTEM_ADMIN),
 * preview 는 조회만이라 [SfSystemPermission.VIEW_ALL_DATA].
 */
@RestController
@RequestMapping("/api/v1/admin/ppt-history/master-id-backfill")
class AdminPPTHistoryMasterIdBackfillController(
    private val service: PPTHistoryMasterIdBackfillService,
) {

    /** 백필 대상 건수 조회 (변경 없음). */
    @GetMapping("/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun preview(): ResponseEntity<ApiResponse<PPTHistoryMasterIdBackfillService.BackfillPreview>> =
        ResponseEntity.ok(ApiResponse.success(service.preview()))

    /**
     * 단일 매칭 이력의 `master_id` 를 채운다. 이력 id 오래된 순 최대 limit 건.
     * @param limit 이번 실행 처리 상한 (기본 1000, 최대 5000 으로 서비스에서 clamp).
     */
    @PostMapping("/execute")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun execute(
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<ApiResponse<PPTHistoryMasterIdBackfillService.BackfillResult>> {
        val result = service.backfill(limit ?: PPTHistoryMasterIdBackfillService.DEFAULT_LIMIT)
        return ResponseEntity.ok(ApiResponse.success(result, "백필을 실행했습니다"))
    }
}
