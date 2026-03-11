package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.OrgResponse
import com.otoki.internal.admin.service.AdminOrganizationService
import com.otoki.internal.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/organizations")
class AdminOrganizationController(
    private val adminOrganizationService: AdminOrganizationService
) {

    @GetMapping
    fun getOrganizations(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) level: String?
    ): ResponseEntity<ApiResponse<List<OrgResponse>>> {
        if (keyword != null && keyword.length > 50) {
            throw IllegalArgumentException("검색어는 50자 이하여야 합니다")
        }
        if (level != null && level !in VALID_LEVELS) {
            throw IllegalArgumentException("유효하지 않은 레벨입니다. L2, L3, L4, L5 중 하나를 입력하세요.")
        }
        val response = adminOrganizationService.getOrganizations(keyword, level)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    companion object {
        private val VALID_LEVELS = setOf("L2", "L3", "L4", "L5")
    }
}
