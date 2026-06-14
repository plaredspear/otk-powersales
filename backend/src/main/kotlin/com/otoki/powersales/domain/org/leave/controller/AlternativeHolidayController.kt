package com.otoki.powersales.domain.org.leave.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayCreateResponse
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayListItemResponse
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayRequest
import com.otoki.powersales.domain.org.leave.service.AlternativeHolidayService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/mobile/alternative-holidays")
class AlternativeHolidayController(
    private val alternativeHolidayService: AlternativeHolidayService
) {

    @PostMapping
    fun createAlternativeHoliday(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AlternativeHolidayRequest
    ): ResponseEntity<ApiResponse<AlternativeHolidayCreateResponse>> {
        val response = alternativeHolidayService.createAlternativeHoliday(
            principal.userId, request.actualWorkDate, request.targetAltHolidayDate
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "대체휴무가 신청되었습니다"))
    }

    @GetMapping
    fun getAlternativeHolidays(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<ApiResponse<List<AlternativeHolidayListItemResponse>>> {
        val response = alternativeHolidayService.getAlternativeHolidays(
            principal.userId, startDate, endDate
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
