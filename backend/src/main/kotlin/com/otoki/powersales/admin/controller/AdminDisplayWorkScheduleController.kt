package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleBatchConfirmRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleBatchDeleteRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleConfirmRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleExportRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleDetailDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleFileRequiredException
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.activity.schedule.service.AdminDisplayWorkScheduleService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/display-work-schedule")
class AdminDisplayWorkScheduleController(
    private val adminDisplayWorkScheduleService: AdminDisplayWorkScheduleService,
) {

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun listSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) confirmed: Boolean?,
        @RequestParam(required = false) typeOfWork3: String?,
        @RequestParam(required = false) startDateFrom: LocalDate?,
        @RequestParam(required = false) startDateTo: LocalDate?,
        @RequestParam(required = false) preset: SchedulePreset?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
    ): ResponseEntity<ApiResponse<Page<ScheduleListItemDto>>> {
        val sort = resolveSort(sortBy, sortDir)
        val result = adminDisplayWorkScheduleService.listSchedules(
            scope, page, size, employeeCode, accountName, confirmed,
            typeOfWork3, startDateFrom, startDateTo, preset, sort
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /** 진열스케줄마스터 검색결과 엑셀 다운로드 — 목록과 동일한 가시 범위/필터로 전량 추출 (최대 건수 제한 적용). */
    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/export-all")
    fun exportAllSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) confirmed: Boolean?,
        @RequestParam(required = false) typeOfWork3: String?,
        @RequestParam(required = false) startDateFrom: LocalDate?,
        @RequestParam(required = false) startDateTo: LocalDate?,
        @RequestParam(required = false) preset: SchedulePreset?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
    ): ResponseEntity<ByteArray> {
        val sort = resolveSort(sortBy, sortDir)
        val result = adminDisplayWorkScheduleService.exportAllSchedules(
            scope, employeeCode, accountName, confirmed,
            typeOfWork3, startDateFrom, startDateTo, preset, sort
        )
        return ExcelResponseUtils.build(result)
    }

    private fun resolveSort(sortBy: String?, sortDir: String?): Sort {
        if (sortBy.isNullOrBlank()) return Sort.unsorted()
        val direction = if (sortDir.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/confirm")
    fun batchConfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.batchConfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/unconfirm")
    fun batchUnconfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.batchUnconfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정 해제되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/{id}")
    fun getScheduleDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ScheduleDetailDto>> {
        val result = adminDisplayWorkScheduleService.getScheduleDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminDisplayWorkScheduleService.updateSchedule(scope, principal.requireEmployeeId(), id, request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 수정되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @PostMapping("/export")
    fun exportSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleExportRequest
    ): ResponseEntity<ByteArray> {
        val result = adminDisplayWorkScheduleService.exportSchedules(scope, request.ids)
        return ExcelResponseUtils.build(result.bytes, result.filename)
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/batch-delete")
    fun batchDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchDeleteResultDto>> {
        val result = adminDisplayWorkScheduleService.batchDelete(scope, principal.requireEmployeeId(), request.ids)
        val message = when {
            result.failedCount == 0 -> "${result.deletedCount}건이 삭제되었습니다"
            result.deletedCount == 0 -> "모든 항목 삭제 실패 (${result.failedCount}건)"
            else -> "${result.deletedCount}건 삭제 / ${result.failedCount}건 실패"
        }
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminDisplayWorkScheduleService.deleteSchedule(scope, principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "스케줄이 삭제되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/template")
    fun downloadTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
    ): ResponseEntity<ByteArray> {
        val result = adminDisplayWorkScheduleService.generateTemplate(scope, principal.requireEmployeeId())
        return ExcelResponseUtils.build(result.bytes, result.filename)
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: AdminScheduleCreateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminDisplayWorkScheduleService.createSchedule(scope, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 등록되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping("/upload")
    fun uploadExcel(
        @CurrentDataScope scope: DataScope,
        @RequestParam("file", required = false) file: MultipartFile?
    ): ResponseEntity<ApiResponse<ScheduleUploadResultDto>> {
        if (file == null) {
            throw ScheduleFileRequiredException()
        }
        val result = adminDisplayWorkScheduleService.uploadAndValidate(scope, file)
        return ResponseEntity.ok(ApiResponse.success(result, "검증 완료"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping("/upload/confirm")
    fun confirmUpload(
        @Valid @RequestBody request: ScheduleConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleConfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.confirmUpload(request.uploadId)
        return ResponseEntity.ok(ApiResponse.success(result, "등록이 완료되었습니다"))
    }
}
