package com.otoki.powersales.domain.activity.promotion.controller

import com.otoki.powersales.domain.activity.promotion.dto.request.DailySalesCloseRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.DailySalesDraftRequest
import com.otoki.powersales.domain.activity.promotion.dto.response.DailySalesFormResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.DailySalesResult
import com.otoki.powersales.domain.activity.promotion.service.MobileDailySalesService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 여사원 일매출 마감/임시저장 API (모바일).
 *
 * 대상은 본인에게 배정된 [com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee] 행 1건이며,
 * promotionEmployeeId 로 식별한다.
 */
@RestController
@RequestMapping("/api/v1/mobile/promotion-employees/{promotionEmployeeId}/daily-sales")
class MobileDailySalesController(
    private val mobileDailySalesService: MobileDailySalesService
) {

    /** 일매출 마감 폼 조회 (임시저장 prefill 포함). */
    @GetMapping
    fun getForm(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionEmployeeId: Long
    ): ResponseEntity<ApiResponse<DailySalesFormResponse>> {
        val result = mobileDailySalesService.getForm(principal.userId, promotionEmployeeId)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /** 일매출 최종 마감. */
    @PostMapping(consumes = ["multipart/form-data"])
    fun close(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionEmployeeId: Long,
        @Valid @ModelAttribute request: DailySalesCloseRequest,
        @RequestParam(required = false) photo: MultipartFile?
    ): ResponseEntity<ApiResponse<DailySalesResult>> {
        val result = mobileDailySalesService.close(principal.userId, promotionEmployeeId, request, photo)
        return ResponseEntity.ok(ApiResponse.success(result, "일매출이 마감되었습니다"))
    }

    /** 일매출 임시저장 (upsert). */
    @PostMapping("/draft", consumes = ["multipart/form-data"])
    fun saveDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionEmployeeId: Long,
        @Valid @ModelAttribute request: DailySalesDraftRequest,
        @RequestParam(required = false) photo: MultipartFile?
    ): ResponseEntity<ApiResponse<DailySalesFormResponse>> {
        val result = mobileDailySalesService.saveDraft(principal.userId, promotionEmployeeId, request, photo)
        return ResponseEntity.ok(ApiResponse.success(result, "임시저장되었습니다"))
    }

    /** 임시저장 폐기. */
    @DeleteMapping("/draft")
    fun deleteDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionEmployeeId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        mobileDailySalesService.deleteDraft(principal.userId, promotionEmployeeId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "임시저장이 삭제되었습니다"))
    }
}
