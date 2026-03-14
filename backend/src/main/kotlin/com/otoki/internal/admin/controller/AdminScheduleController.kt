package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.ScheduleConfirmRequest
import com.otoki.internal.admin.dto.response.BranchDto
import com.otoki.internal.admin.dto.response.ScheduleConfirmResultDto
import com.otoki.internal.admin.dto.response.ScheduleUploadResultDto
import com.otoki.internal.admin.exception.ScheduleFileRequiredException
import com.otoki.internal.admin.service.AdminScheduleService
import com.otoki.internal.admin.service.MissingCostCenterCodeException
import com.otoki.internal.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/admin/schedule")
class AdminScheduleController(
    private val adminScheduleService: AdminScheduleService
) {

    @GetMapping("/branches")
    fun getBranches(): ResponseEntity<ApiResponse<List<BranchDto>>> {
        val branches = adminScheduleService.getBranches()
        return ResponseEntity.ok(ApiResponse.success(branches))
    }

    @GetMapping("/template")
    fun downloadTemplate(
        @RequestParam(required = false) costCenterCode: String?
    ): ResponseEntity<ByteArray> {
        if (costCenterCode.isNullOrBlank()) {
            throw MissingCostCenterCodeException()
        }

        val result = adminScheduleService.generateTemplate(costCenterCode)

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

    @PostMapping("/upload/confirm")
    fun confirmUpload(
        @Valid @RequestBody request: ScheduleConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleConfirmResultDto>> {
        val result = adminScheduleService.confirmUpload(request.uploadId)
        return ResponseEntity.ok(ApiResponse.success(result, "등록이 완료되었습니다"))
    }
}
