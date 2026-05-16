package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.organization.dto.response.OrganizationResponse
import com.otoki.powersales.organization.service.AdminOrganizationService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/organizations")
class AdminOrganizationController(
    private val adminOrganizationService: AdminOrganizationService,
    // WebAdminContextFilter 가 요청 진입 시 산출한 DataScope 를 1회 읽어 service 에 explicit
    // parameter 로 전달.
    private val dataScopeHolder: DataScopeHolder,
) {

    @GetMapping
    fun getOrganizations(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) level: String?
    ): ResponseEntity<ApiResponse<List<OrganizationResponse>>> {
        if (keyword != null && keyword.length > 50) {
            throw IllegalArgumentException("검색어는 50자 이하여야 합니다")
        }
        if (level != null && level !in VALID_LEVELS) {
            throw IllegalArgumentException("유효하지 않은 레벨입니다. L2, L3, L4, L5 중 하나를 입력하세요.")
        }
        val response = adminOrganizationService.getOrganizations(dataScopeHolder.require(), keyword, level)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    companion object {
        private val VALID_LEVELS = setOf("L2", "L3", "L4", "L5")
    }
}
