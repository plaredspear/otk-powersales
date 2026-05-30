package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.schedule.dto.response.FemaleEmployeePlacementCheckResponse
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeWorkHistoryResponse
import com.otoki.powersales.schedule.service.AdminFemaleEmployeePlacementCheckService
import com.otoki.powersales.schedule.service.AdminFemaleEmployeeWorkHistoryService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 여사원 배치 점검 현황 (영업지원실용) — Spec #839.
 *
 * SF Report `InternalSalesReportFolder/new_report_4Ic` 이식. 권한 entity 는 `team_member_schedule`
 * (데이터 소스 = 여사원일정). 여사원 현황 [AdminFemaleEmployeeController] (entity `employee`) 와 권한 entity 가
 * 다르므로 base path 는 공유하되 별도 컨트롤러로 분리.
 */
@RestController
@RequestMapping("/api/v1/admin/female-employees")
class AdminFemaleEmployeePlacementCheckController(
    private val service: AdminFemaleEmployeePlacementCheckService,
    private val workHistoryService: AdminFemaleEmployeeWorkHistoryService,
) {

    /** 월간 배치 점검 조회 (퇴직자 포함 · 여사원/조장). */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/placement-check")
    fun getPlacementCheck(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false, defaultValue = "") costCenterCodes: List<String>,
    ): ResponseEntity<ApiResponse<FemaleEmployeePlacementCheckResponse>> {
        val response = service.getPlacementCheck(scope, year, month, costCenterCodes)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 배치 점검 엑셀 다운로드. */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/placement-check/export")
    fun exportPlacementCheck(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false, defaultValue = "") costCenterCodes: List<String>,
    ): ResponseEntity<ByteArray> {
        val result = service.exportPlacementCheck(scope, year, month, costCenterCodes)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val encodedFilename = URLEncoder.encode(result.filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }

    /** 개인별(사번) 월간 근무내역 조회 (Spec #840). */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/work-history")
    fun getWorkHistory(
        @CurrentDataScope scope: DataScope,
        @RequestParam employeeCode: String,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<FemaleEmployeeWorkHistoryResponse>> {
        val response = workHistoryService.getWorkHistory(scope, employeeCode, year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 개인별 근무내역 엑셀 다운로드. */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/work-history/export")
    fun exportWorkHistory(
        @CurrentDataScope scope: DataScope,
        @RequestParam employeeCode: String,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ByteArray> {
        val result = workHistoryService.exportWorkHistory(scope, employeeCode, year, month)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val encodedFilename = URLEncoder.encode(result.filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }
}
