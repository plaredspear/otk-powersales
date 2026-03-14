package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.BranchDto
import com.otoki.internal.admin.service.AdminScheduleService
import com.otoki.internal.admin.service.MissingCostCenterCodeException
import com.otoki.internal.common.dto.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}
