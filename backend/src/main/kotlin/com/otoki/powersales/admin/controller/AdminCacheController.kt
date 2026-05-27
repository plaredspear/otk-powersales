package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.service.AdminCacheService
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 운영도구 - Redis 캐시 evict admin endpoint.
 *
 * 운영자 (SYSTEM_ADMIN) 가 web 운영도구 화면에서 cache name 단위로 즉시 evict 할 수 있도록 한다.
 * 사용 시나리오는 [com.otoki.powersales.admin.service.AdminCacheService] 의 클래스 주석 참조.
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
class AdminCacheController(
    private val adminCacheService: AdminCacheService,
) {

    @GetMapping
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun list(): ResponseEntity<ApiResponse<List<AdminCacheService.CacheInfo>>> {
        return ResponseEntity.ok(ApiResponse.success(adminCacheService.listCaches()))
    }

    @PostMapping("/{cacheName}/evict")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun evict(
        @PathVariable cacheName: String,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AdminCacheService.EvictResult>> {
        val result = adminCacheService.evict(cacheName, principal.employeeCode ?: "unknown")
        return ResponseEntity.ok(ApiResponse.success(result, "캐시가 무효화되었습니다"))
    }
}
