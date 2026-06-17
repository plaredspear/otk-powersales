package com.otoki.powersales.domain.org.employee.controller

import com.otoki.powersales.domain.org.employee.dto.response.StaffEvaluationResponse
import com.otoki.powersales.domain.org.employee.service.StaffEvaluationService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 여사원 평가조회 API Controller.
 *
 * 레거시 `GET /employee/evaluationList` (+ `evaluationListAjax`) 정합 — 인증 사용자 본인의
 * 거래처별 목표/실적/달성률 + 지점평가 점수를 조회. `yearMonth` 미지정 시 전월.
 */
@RestController
@RequestMapping("/api/v1/mobile/staff-evaluation")
class StaffEvaluationController(
    private val staffEvaluationService: StaffEvaluationService,
) {

    /**
     * 여사원 평가조회.
     * GET /api/v1/mobile/staff-evaluation?yearMonth=YYYYMM
     *
     * 사번/세션 대신 인증 사용자(`principal.userId`) 본인 데이터만 조회한다 (레거시 세션 sfid 정합).
     */
    @GetMapping
    fun getStaffEvaluation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) yearMonth: String?,
    ): ResponseEntity<ApiResponse<StaffEvaluationResponse>> {
        val response = staffEvaluationService.getEvaluation(principal.userId, yearMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
