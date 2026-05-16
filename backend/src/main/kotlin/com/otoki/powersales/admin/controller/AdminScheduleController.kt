package com.otoki.powersales.admin.controller

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
import com.otoki.powersales.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.schedule.enums.SchedulePreset
import com.otoki.powersales.schedule.exception.ScheduleFileRequiredException
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.schedule.service.AdminScheduleService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
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
    // WebAdminContextFilter 가 요청 진입 시 산출한 DataScope 를 1회 읽어 service 에 explicit
    // parameter 로 전달. service 는 holder/ambient context 비인지로 유지.
    private val dataScopeHolder: DataScopeHolder,
) {

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/list")
    fun listSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
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
            dataScopeHolder.require(), page, size, employeeCode, accountName, confirmed,
            typeOfWork3, startDateFrom, startDateTo, preset, sort
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    private fun resolveSort(sortBy: String?, sortDir: String?): Sort {
        if (sortBy.isNullOrBlank()) return Sort.unsorted()
        val direction = if (sortDir.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PatchMapping("/confirm")
    fun batchConfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminScheduleService.batchConfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PatchMapping("/unconfirm")
    fun batchUnconfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminScheduleService.batchUnconfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정 해제되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminScheduleService.updateSchedule(dataScopeHolder.require(), principal.requireEmployeeId(), id, request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 수정되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @PostMapping("/export")
    fun exportSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: ScheduleExportRequest
    ): ResponseEntity<ByteArray> {
        val result = adminScheduleService.exportSchedules(dataScopeHolder.require(), request.ids)

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

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping("/batch-delete")
    fun batchDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: ScheduleBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchDeleteResultDto>> {
        val result = adminScheduleService.batchDelete(dataScopeHolder.require(), principal.requireEmployeeId(), request.ids)
        val message = when {
            result.failedCount == 0 -> "${result.deletedCount}건이 삭제되었습니다"
            result.deletedCount == 0 -> "모든 항목 삭제 실패 (${result.failedCount}건)"
            else -> "${result.deletedCount}건 삭제 / ${result.failedCount}건 실패"
        }
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminScheduleService.deleteSchedule(dataScopeHolder.require(), principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "스케줄이 삭제되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
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

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminScheduleCreateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminScheduleService.createSchedule(dataScopeHolder.require(), principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 등록되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping("/upload")
    fun uploadExcel(
        @RequestParam("file", required = false) file: MultipartFile?
    ): ResponseEntity<ApiResponse<ScheduleUploadResultDto>> {
        if (file == null) {
            throw ScheduleFileRequiredException()
        }
        val result = adminScheduleService.uploadAndValidate(file)
        return ResponseEntity.ok(ApiResponse.success(result, "검증 완료"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping("/upload/confirm")
    fun confirmUpload(
        @Valid @RequestBody request: ScheduleConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleConfirmResultDto>> {
        val result = adminScheduleService.confirmUpload(request.uploadId)
        return ResponseEntity.ok(ApiResponse.success(result, "등록이 완료되었습니다"))
    }
}
