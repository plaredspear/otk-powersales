package com.otoki.powersales.sfmigration.controller

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sfmigration.dto.SfFkResolveProgressResponse
import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.service.SfFkResolveProgress
import com.otoki.powersales.sfmigration.service.SfMigrationStage2FkService
import com.otoki.powersales.sfmigration.service.SfMigrationStage2Service
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

/**
 * SF 데이터 마이그레이션 Stage 2 admin 엔드포인트 (1회성 cut-over).
 *
 * 권한: SYSTEM_ADMIN (= ROLE_ADMIN GrantedAuthority) 만 호출 가능.
 * 노출 substep: fk (2-A) / picklist (2-B) / password (2-C) / permission (2-D).
 *
 * fk substep 만 비동기 실행 (대용량 ErpOrderProduct 등 수분~수십분 소요) + 진행 상태 polling 지원.
 */
@RestController
class SfMigrationStage2Controller(
    private val service: SfMigrationStage2Service,
    private val fkService: SfMigrationStage2FkService,
    private val fkProgress: SfFkResolveProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val fkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sf-fk-resolve").apply { isDaemon = true }
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/fk")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runFkResolve(): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        if (fkProgress.status == SfFkResolveProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(fkProgress.toResponse()))
        }
        fkExecutor.submit {
            try {
                fkService.runFkResolve()
            } catch (e: Exception) {
                log.error("[fk] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(fkProgress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage2/fk/progress")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun getFkProgress(): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        return ResponseEntity.ok(ApiResponse.success(fkProgress.toResponse()))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runPicklistMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPicklistMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/password")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runPasswordHash(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPasswordHash()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/permission")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runPermissionMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPermissionMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun SfFkResolveProgress.toResponse(): SfFkResolveProgressResponse {
        return SfFkResolveProgressResponse(
            status = status.name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalTables = totalTables,
            completedTables = completedTablesCount,
            currentTable = currentTable,
            currentTableChunk = currentTableChunk,
            currentTableTotalChunks = currentTableTotalChunks,
            totalRowsAffected = totalRowsAffected,
            tableResults = tableResults.toList(),
            errors = errors.toList(),
        )
    }
}
