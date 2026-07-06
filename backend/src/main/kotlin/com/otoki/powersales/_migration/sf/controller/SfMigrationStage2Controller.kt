package com.otoki.powersales._migration.sf.controller

import com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyTraversal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales._migration.sf.dto.SfFkResolveProgressResponse
import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales._migration.sf.service.SfFkResolveProgress
import com.otoki.powersales._migration.sf.service.SfMigrationStage2FkService
import com.otoki.powersales._migration.sf.service.SfMigrationStage2NaturalKeyFkService
import com.otoki.powersales._migration.sf.service.SfMigrationStage2Service
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * SF 데이터 마이그레이션 Stage 2 admin 엔드포인트 (1회성 cut-over).
 *
 * 권한: 로그인(authenticated)만 요구 — @RequiresSfPermission 제거. 마이그레이션 자체가 권한 데이터를
 * 적재하는 단계라 MODIFY_ALL_DATA 권한 부트스트랩이 닭-달걀 문제를 일으켜, 일회성 운영 도구로서
 * 가드를 떼고 web 사이드 메뉴에서도 제외 (URL 직접 진입). 마이그레이션 완료 후 가드 복원 권장.
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
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: com.otoki.powersales.admin.security.AdminDataScopeCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val fkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sf-fk-resolve").apply { isDaemon = true }
    }

    /**
     * @param tableName null/미지정 시 전체 테이블, 지정 시 해당 테이블 1개만 처리.
     *   처리 가능한 테이블 목록은 `GET .../stage2/fk/tables` 로 조회.
     *   단일 테이블도 동일한 single-thread executor + progress 메커니즘을 공유한다.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/fk")
    fun runFkResolve(
        @RequestParam(name = "tableName", required = false) tableName: String?,
    ): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        // 동시 실행 차단 — 요청 스레드에서 원자적으로 RUNNING 을 선점한다. submit 후 워커의 begin() 에
        // status 세팅을 맡기면, 선점~begin() 사이에 두 요청이 모두 가드를 통과하는 TOCTOU 구멍이 생긴다
        // (single-thread executor 라 순차 실행되긴 하나 동시 1회 의도와 어긋나고, 두 번째 202 응답이
        // 직전 실행의 stale 진행 상태를 노출했다). tryAcquire 로 선점에 실패하면 진행 중이므로 거부한다.
        if (!fkProgress.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(fkProgress.toResponse()))
        }
        try {
            fkExecutor.submit {
                try {
                    fkService.runFkResolve(tableName?.takeIf { it.isNotBlank() })
                    // FK resolve 가 permission_set_assignment.user_id 등 권한 FK 를 채운 직후 — stale
                    // 권한 캐시 무효화 (마이그레이션 직후 권한 어긋남 방지).
                    adminPermissionCache.invalidateAll()
                    adminDataScopeCache.invalidateAll()
                } catch (e: Exception) {
                    log.error("[fk] async run failed", e)
                }
            }
        } catch (e: RejectedExecutionException) {
            // 스레드풀이 작업을 거부하면 워커가 begin()/finish*() 를 호출하지 않아 RUNNING 이 영구히
            // 남는다 — 선점한 락을 즉시 되돌려 다음 요청이 가능하게 한다.
            fkProgress.releaseWithoutRun()
            log.error("[fk] executor rejected the task", e)
            throw e
        }
        // tryAcquire 가 이미 이번 실행용으로 progress 를 비우고 RUNNING 으로 전이했으므로,
        // 이 202 응답 body 는 항상 "방금 시작한 이 실행" 을 가리킨다 (옛 결과 노출 없음).
        return ResponseEntity.accepted().body(ApiResponse.success(fkProgress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage2/fk/progress")
    fun getFkProgress(): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        // Redis 스냅샷 우선 — 다중 인스턴스에서 실행 인스턴스가 아닌 곳으로 polling 이 라우팅돼도 진행 상태 조회.
        return ResponseEntity.ok(ApiResponse.success(fkProgress.loadResponse()))
    }

    /**
     * 처리 가능한 테이블 목록 (web 드롭다운에서 단일 테이블 선택용). 정렬된 테이블명 배열.
     */
    @GetMapping("/api/v1/admin/sf-migration/stage2/fk/tables")
    fun getFkResolvableTables(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(fkService.listResolvableTables()))
    }

    /**
     * spec #800 — Natural Key FK Resolve.
     *
     * `NATURAL_KEY_FK_MAPPINGS` (8 entry) 일괄 적용. sfid prefix 기반이 아니라
     * developer_name / name / 외부 sfid 컬럼 기반 join 으로 id 채움.
     *
     * 운영 cut-over 시점에 fk substep 직후 1회 호출.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/fk-natural-key")
    fun runNaturalKeyFkResolve(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = naturalKeyFkService.runNaturalKeyFkResolve()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * UploadFile polymorphic parent resolve — record_sfid (SF Id text) → parent_id (Long FK).
     *
     * fk substep 직후 1회 호출. 매핑 표는 [com.otoki.powersales.platform.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS].
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent")
    fun runUploadFilePolymorphicParent(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runUploadFilePolymorphicParent()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 공지 본문 rtaImage <img> → placeholder 치환 (notice.contents UPDATE).
     *
     * 공지 본문에 박힌 SF rtaImage 서블릿 URL 을 만료 없는 placeholder
     * (`<img src="notice-image://{refid}" data-refid="{refid}">`) 로 치환한다. 조회 시점에
     * NoticeService 가 data-refid 로 presigned URL 을 rewrite 하므로, 본문에는 placeholder 만 영구 저장한다.
     *
     * 공지 본문 이미지 적재(Stage1 NoticeImageUploadFile) + UploadFile Parent Resolve 완료 후 1회 실행.
     * 기본 dryRun=true (변경 대상 집계만). 실제 UPDATE 는 dryRun=false 명시 시에만. 멱등 (이미 치환된 본문 skip).
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/notice-rta-placeholder")
    fun runNoticeRtaPlaceholder(
        @RequestParam(name = "dryRun", defaultValue = "true") dryRun: Boolean,
    ): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runNoticeRtaPlaceholderRewrite(dryRun)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist")
    fun runPicklistMapping(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPicklistMapping()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * Stage 2-B 의 1개 컬럼 개별 실행. column 값:
     * - `user_cost_center_code`
     *
     * spec #807 이후 `employee_role` 분기 폐기 — SF AppAuthority picklist value 가 곧 저장값.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/picklist/{column}")
    fun runPicklistColumn(
        @PathVariable column: String,
    ): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = when (column) {
            "user_cost_center_code" -> service.runUserCostCenterCodeSync()
            "ppt_master_branch_code" -> service.runPptMasterBranchCodeSync()
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
    fun runPasswordHash(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runPasswordHash()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * User.profile_id 를 SF User.ProfileId(=profile_sfid) 기준으로 최종 정합.
     *
     * SAP 인바운드 provisioning fallback(`5.영업사원`/`9. Staff`)이 profile_id 를 선점하면 일반 FK Resolve 의
     * `IS NULL` 가드가 SF 실제 Profile(`6.조장` 등)로 갱신하지 못하는 정합 사고를 교정한다. `profile_sfid`
     * 보유 사원의 profile_id 를 SF 정답으로 무조건 override (COALESCE 없음). '시스템 관리자' 격상 계정은 보존.
     *
     * 운영 cut-over 시점에 fk substep(2-A) **직후 1회** 호출. 멱등.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/user-profile-reconcile")
    fun runUserProfileSfidReconcile(): ResponseEntity<ApiResponse<SfMigrationStage2Response>> {
        val response = service.runUserProfileSfidReconcile()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * spec #790 — UserRole hierarchy snapshot 재계산 endpoint.
     *
     * cut-over 시점에 user_role 적재 + Stage 2 fk substep 이 모든 user_role_id 를 채운 후 1회 호출.
     * 본 endpoint 는 [UserRoleHierarchyTraversal.recomputeAll] wrapper — `user_role.parent_user_role_id`
     * 트리 기반으로 `all_subordinate_ids` (jsonb) + `depth` + `ancestor_path` + `snapshot_at` 재계산.
     *
     * 운영 후 admin 사용자가 UserRole entity 를 변경하는 Service 가 [com.otoki.powersales.platform.auth.sharing.event.UserRoleChangedEvent]
     * 를 명시 발행하면 [com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyEventHandler] 가 자동 invalidate.
     * 본 endpoint 는 그 자동화 메커니즘과 별개로 batch 1회 / incident 복구 용도.
     */
    @PostMapping("/api/v1/admin/sf-migration/stage2/user-role-hierarchy")
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
}
