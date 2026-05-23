package com.otoki.powersales.sfmigration.controller

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.auth.sharing.service.UserRoleHierarchyTraversal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sfmigration.dto.SfFkResolveProgressResponse
import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.dto.SubstepResult
import com.otoki.powersales.sfmigration.service.SfFkResolveProgress
import com.otoki.powersales.sfmigration.service.SfMigrationStage2FkService
import com.otoki.powersales.sfmigration.service.SfMigrationStage2NaturalKeyFkService
import com.otoki.powersales.sfmigration.service.SfMigrationStage2Service
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val naturalKeyFkService: SfMigrationStage2NaturalKeyFkService,
    private val fkProgress: SfFkResolveProgress,
    private val hierarchyTraversal: UserRoleHierarchyTraversal,
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

    /**
     * spec #800 — Natural Key FK Resolve.
     *
     * `NATURAL_KEY_FK_MAPPINGS` (9 entry) 일괄 적용. sfid prefix 기반이 아니라
     * developer_name / name / 외부 sfid 컬럼 기반 join 으로 id 채움.
     *
     * 운영 cut-over 시점에 fk substep 직후 1회 호출.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/fk-natural-key")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runNaturalKeyFkResolve(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = naturalKeyFkService.runNaturalKeyFkResolve()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runPicklistMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPicklistMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * Stage 2-B 의 4개 컬럼 중 1개만 개별 실행. column 값:
     * - `employee_role` / `employee_ppt` / `user_profile_type` / `user_cost_center_code`
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist/{column}")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runPicklistColumn(
        @PathVariable column: String,
    ): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = when (column) {
            "employee_role" -> service.runPicklistEmployeeRole()
            "employee_ppt" -> service.runPicklistEmployeePpt()
            "user_profile_type" -> service.runPicklistUserProfileType()
            "user_cost_center_code" -> service.runUserCostCenterCodeSync()
            else -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.success(
                    SfMigrationStage2Response(
                        substep = "picklist.$column",
                        results = emptyList(),
                        totalRowsAffected = 0,
                    )
                )
            )
        }
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

    /**
     * spec #790 — UserRole hierarchy snapshot 재계산 endpoint.
     *
     * cut-over 시점에 user_role 적재 + Stage 2 fk substep 이 모든 user_role_id 를 채운 후 1회 호출.
     * 본 endpoint 는 [UserRoleHierarchyTraversal.recomputeAll] wrapper — `user_role.parent_user_role_id`
     * 트리 기반으로 `all_subordinate_ids` (jsonb) + `depth` + `ancestor_path` + `snapshot_at` 재계산.
     *
     * 운영 후 admin 사용자가 UserRole entity 를 변경하는 Service 가 [com.otoki.powersales.auth.sharing.event.UserRoleChangedEvent]
     * 를 명시 발행하면 [com.otoki.powersales.auth.sharing.service.UserRoleHierarchyEventHandler] 가 자동 invalidate.
     * 본 endpoint 는 그 자동화 메커니즘과 별개로 batch 1회 / incident 복구 용도.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/user-role-hierarchy")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun runUserRoleHierarchyRecalc(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        log.info("[user-role-hierarchy] recompute all triggered by admin endpoint")
        val before = System.currentTimeMillis()
        hierarchyTraversal.recomputeAll()
        val elapsed = System.currentTimeMillis() - before
        log.info("[user-role-hierarchy] recompute all complete in {} ms", elapsed)

        // recomputeAll 은 row 카운트 반환 안 함 — Service 내부 logger 가 상세 박제.
        // 응답은 wrapper 한정으로 totalRowsAffected = 0 (의미: 본 endpoint 는 row 변경 카운트가 아니라
        // snapshot 재계산 트리거 — 실측 변경 row 수는 logger 에서 확인).
        val response = SfMigrationStage2Response(
            substep = "userRoleHierarchy",
            results = listOf(
                SubstepResult(
                    label = "user_role_hierarchy_snapshot.recalculated",
                    rowsAffected = 0,
                ),
            ),
            totalRowsAffected = 0,
        )
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
