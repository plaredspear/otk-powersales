package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.HolidayMasterCreateRequest
import com.otoki.powersales.admin.dto.request.HolidayMasterUpdateRequest
import com.otoki.powersales.admin.dto.response.HolidayMasterResponse
import com.otoki.powersales.leave.service.AdminHolidayMasterService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/holiday-masters")
class AdminHolidayMasterController(
    private val adminHolidayMasterService: AdminHolidayMasterService
) {

    @GetMapping
    fun getHolidayMasters(
        @RequestParam year: Int
    ): ResponseEntity<ApiResponse<List<HolidayMasterResponse>>> {
        val response = adminHolidayMasterService.getHolidayMasters(year)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    fun createHolidayMaster(
        @Valid @RequestBody request: HolidayMasterCreateRequest
    ): ResponseEntity<ApiResponse<HolidayMasterResponse>> {
        val response = adminHolidayMasterService.createHolidayMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    fun updateHolidayMaster(
        @PathVariable id: Long,
        @Valid @RequestBody request: HolidayMasterUpdateRequest
    ): ResponseEntity<ApiResponse<HolidayMasterResponse>> {
        val response = adminHolidayMasterService.updateHolidayMaster(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    fun deleteHolidayMaster(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminHolidayMasterService.deleteHolidayMaster(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
