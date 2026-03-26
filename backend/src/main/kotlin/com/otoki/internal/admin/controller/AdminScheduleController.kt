package com.otoki.internal.admin.controller

import com.otoki.internal.schedule.dto.request.ScheduleBatchConfirmRequest
import com.otoki.internal.schedule.dto.request.ScheduleConfirmRequest
import com.otoki.internal.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.internal.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.internal.schedule.dto.response.ScheduleListItemDto
import com.otoki.internal.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.internal.schedule.exception.ScheduleFileRequiredException
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.schedule.service.AdminScheduleService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
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
    private val adminScheduleService: AdminScheduleService
) {

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/list")
    fun listSchedules(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) confirmed: Boolean?,
        @RequestParam(required = false) typeOfWork3: String?,
        @RequestParam(required = false) startDateFrom: LocalDate?,
        @RequestParam(required = false) startDateTo: LocalDate?
    ): ResponseEntity<ApiResponse<Page<ScheduleListItemDto>>> {
        val result = adminScheduleService.listSchedules(
            page, size, employeeCode, accountName, confirmed, typeOfWork3, startDateFrom, startDateTo
        )
        return ResponseEntity.ok(ApiResponse.success(result))
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

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/template")
    fun downloadTemplate(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ByteArray> {
        val result = adminScheduleService.generateTemplate(principal.userId)

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
