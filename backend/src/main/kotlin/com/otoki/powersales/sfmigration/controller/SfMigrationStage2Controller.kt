package com.otoki.powersales.sfmigration.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.service.SfMigrationStage2FkService
import com.otoki.powersales.sfmigration.service.SfMigrationStage2Service
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SF 데이터 마이그레이션 Stage 2 admin 엔드포인트 (1회성 cut-over).
 *
 * 권한: SYSTEM_ADMIN (= ROLE_ADMIN GrantedAuthority) 만 호출 가능.
 * 노출 substep: fk (2-A) / picklist (2-B) / password (2-C) / permission (2-D).
 */
@RestController
class SfMigrationStage2Controller(
    private val service: SfMigrationStage2Service,
    private val fkService: SfMigrationStage2FkService,
) {

    @PostMapping("/api/v1/admin/sf-migration/stage2/fk")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun runFkResolve(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = fkService.runFkResolve()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun runPicklistMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPicklistMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/password")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun runPasswordHash(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPasswordHash()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/permission")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun runPermissionMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPermissionMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
