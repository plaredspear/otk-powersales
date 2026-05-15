package com.otoki.powersales.admin.controller

import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkDeleteRequest
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkUpdateRequest
import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkDeleteResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkUpdateResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleListResponse
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.schedule.service.AdminPromotionScheduleService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 행사 단위 일정 관리 컨트롤러 (Spec #571 P1-B).
 */
@RestController
@RequestMapping("/api/v1/admin/promotions")
class AdminPromotionScheduleController(
    private val adminPromotionScheduleService: AdminPromotionScheduleService
) {

    @GetMapping("/{promotionId}/schedules")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<ApiResponse<PromotionScheduleListResponse>> {
        val response = adminPromotionScheduleService.getSchedules(promotionId, startDate, endDate)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{promotionId}/schedules/bulk")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun bulkUpdate(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: PromotionScheduleBulkUpdateRequest
    ): ResponseEntity<ApiResponse<PromotionScheduleBulkUpdateResponse>> {
        val response = adminPromotionScheduleService.bulkUpdate(promotionId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "일정이 변경되었습니다"))
    }

    @DeleteMapping("/{promotionId}/schedules/bulk")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun bulkDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: PromotionScheduleBulkDeleteRequest
    ): ResponseEntity<ApiResponse<PromotionScheduleBulkDeleteResponse>> {
        val response = adminPromotionScheduleService.bulkDelete(promotionId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "일정이 삭제되었습니다"))
    }
}
