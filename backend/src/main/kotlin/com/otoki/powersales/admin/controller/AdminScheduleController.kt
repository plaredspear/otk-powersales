package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.request.ScheduleBatchConfirmRequest
import com.otoki.powersales.schedule.dto.request.ScheduleBatchDeleteRequest
import com.otoki.powersales.schedule.dto.request.ScheduleConfirmRequest
import com.otoki.powersales.schedule.dto.request.ScheduleExportRequest
import com.otoki.powersales.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleDetailDto
import com.otoki.powersales.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.schedule.enums.SchedulePreset
import com.otoki.powersales.schedule.exception.ScheduleFileRequiredException
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.schedule.service.AdminScheduleService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/schedule")
class AdminScheduleController(
    private val adminScheduleService: AdminScheduleService,
) {

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
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
        val result = adminScheduleService.listSchedules(
            scope, page, size, employeeCode, accountName, confirmed,
            typeOfWork3, startDateFrom, startDateTo, preset, sort
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    private fun resolveSort(sortBy: String?, sortDir: String?): Sort {
        if (sortBy.isNullOrBlank()) return Sort.unsorted()
        val direction = if (sortDir.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/confirm")
    fun batchConfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminScheduleService.batchConfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/unconfirm")
    fun batchUnconfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminScheduleService.batchUnconfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정 해제되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/{id}")
    fun getScheduleDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ScheduleDetailDto>> {
        val result = adminScheduleService.getScheduleDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminScheduleService.updateSchedule(scope, principal.requireEmployeeId(), id, request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 수정되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @PostMapping("/export")
    fun exportSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleExportRequest
    ): ResponseEntity<ByteArray> {
        val result = adminScheduleService.exportSchedules(scope, request.ids)

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        headers.setContentDispositionFormData("attachment", result.filename)
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")

        return ResponseEntity.ok()
            .headers(headers)
            .body(result.bytes)
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/batch-delete")
    fun batchDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchDeleteResultDto>> {
        val result = adminScheduleService.batchDelete(scope, principal.requireEmployeeId(), request.ids)
        val message = when {
            result.failedCount == 0 -> "${result.deletedCount}건이 삭제되었습니다"
            result.deletedCount == 0 -> "모든 항목 삭제 실패 (${result.failedCount}건)"
            else -> "${result.deletedCount}건 삭제 / ${result.failedCount}건 실패"
        }
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminScheduleService.deleteSchedule(scope, principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "스케줄이 삭제되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/template")
    fun downloadTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ByteArray> {
        val result = adminScheduleService.generateTemplate(principal.requireEmployeeId())

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        headers.setContentDispositionFormData("attachment", result.filename)
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")

        return ResponseEntity.ok()
            .headers(headers)
            .body(result.bytes)
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: AdminScheduleCreateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminScheduleService.createSchedule(scope, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 등록되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/upload")
    fun uploadExcel(
        @CurrentDataScope scope: DataScope,
        @RequestParam("file", required = false) file: MultipartFile?
    ): ResponseEntity<ApiResponse<ScheduleUploadResultDto>> {
        if (file == null) {
            throw ScheduleFileRequiredException()
        }
        val result = adminScheduleService.uploadAndValidate(scope, file)
        return ResponseEntity.ok(ApiResponse.success(result, "검증 완료"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/upload/confirm")
    fun confirmUpload(
        @Valid @RequestBody request: ScheduleConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleConfirmResultDto>> {
        val result = adminScheduleService.confirmUpload(request.uploadId)
        return ResponseEntity.ok(ApiResponse.success(result, "등록이 완료되었습니다"))
    }
}
