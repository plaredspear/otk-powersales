package com.otoki.powersales._migration.sf.stage1

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

/**
 * SF migration Stage 1 — S3 의 SF export CSV 를 backend 가 직접 PostgreSQL 에 COPY 적재.
 *
 * 권한: SYSTEM_ADMIN (= `SF_MIGRATION_RUN` permission, Stage 2 fk 와 동일).
 *
 * 사전 조건:
 *  - 사용자가 extract-csv.sh 산출 CSV 를 S3 에 업로드해둔 상태.
 *  - DB 는 비어있거나 (첫 적재) ON CONFLICT DO NOTHING 으로 멱등 (재적재).
 *
 * 비동기 실행 — 단일 thread executor 로 동시 1회만 enforce. RUNNING 중 신규 요청은 409.
 * 진행 상태는 GET /progress endpoint 로 polling.
 *
 * 진입점 2개:
 *  - POST /copy-from-s3       : 단건 (target 1개) 실행
 *  - POST /copy-all-from-s3   : 일괄 (등록된 모든 entity) 실행 — 의존성 순서, continue-on-error
 *                               (1개 실패해도 중단 없이 다음 entity 계속, 실패는 누적)
 *
 * 권한: 로그인(authenticated)만 요구 — @RequiresSfPermission 제거. 권한 데이터 적재 단계라
 * MODIFY_ALL_DATA 부트스트랩 닭-달걀 회피. web 사이드 메뉴 제외 + URL 직접 진입. 완료 후 가드 복원 권장.
 */
// Stage1S3CopyService 와 동일하게 dev/prod 에서만 활성화 (S3Client 의존 + 운영 도구 성격).
@RestController
@Profile("dev | prod")
class Stage1CopyController(
    private val service: Stage1S3CopyService,
    private val progress: Stage1CopyProgress,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: com.otoki.powersales.admin.security.AdminDataScopeCache,
    private val branchCodeExpander: com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander,
    // 운영 S3 bucket (EB 콘솔 환경 속성 S3_BUCKET). Stage1 CSV 도 동일 bucket 사용 — UI 프리필.
    @Value("\${app.aws.s3.bucket:}") private val configuredS3Bucket: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sf-stage1-copy").apply { isDaemon = true }
    }

    @PostMapping("/api/v1/admin/sf-migration/stage1/copy-from-s3")
    fun copyFromS3(
        @Valid @RequestBody req: Stage1CopyRequest,
    ): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        if (progress.status == Stage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        // 파일명은 target 의 csvFileName 으로 자동 조립 (BATCH 모드와 대칭). 매핑 SoT = Stage1Targets.
        val meta = Stage1Targets.get(req.targetName)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("UNKNOWN_TARGET", "Unknown target: ${req.targetName}"))
        val s3Key = "${req.s3KeyPrefix.removeSuffix("/")}/${meta.csvFileName}"
        progress.begin(req.targetName, req.s3Bucket, s3Key)
        executor.submit {
            try {
                service.copyFromS3(req.targetName, req.s3Bucket, s3Key, req.maxRows)
                // 권한 원천 테이블 적재 시 stale 권한 캐시 무효화 (마이그레이션 직후 권한 어긋남 방지).
                if (Stage1Targets.affectsPermissionCache(req.targetName)) {
                    adminPermissionCache.invalidateAll()
                    adminDataScopeCache.invalidateAll()
                }
                // BranchMapping 적재 시 BranchCodeExpander 의 부팅 1회 캐시 재빌드 (stale 방지).
                if (Stage1Targets.affectsBranchCodeCache(req.targetName)) {
                    branchCodeExpander.reload()
                }
            } catch (e: Exception) {
                log.error("[stage1-copy] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage1/copy-all-from-s3")
    fun copyAllFromS3(
        @Valid @RequestBody req: Stage1CopyAllRequest,
    ): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        if (progress.status == Stage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        progress.beginBatch(req.s3Bucket, Stage1Targets.list())
        executor.submit {
            try {
                service.copyAllFromS3(req.s3Bucket, req.s3KeyPrefix, req.maxRows)
                // 일괄 적재는 권한 원천 테이블 (Profile / PermissionSet* / User 등) 을 항상 포함 →
                // stale 권한 캐시 무효화.
                adminPermissionCache.invalidateAll()
                adminDataScopeCache.invalidateAll()
                // 일괄 적재는 BranchMapping 도 항상 포함 → BranchCodeExpander 캐시 재빌드 (stale 방지).
                branchCodeExpander.reload()
            } catch (e: Exception) {
                log.error("[stage1-copy-all] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage1/copy-from-s3/progress")
    fun getProgress(): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        // Redis 스냅샷 우선 — 다중 인스턴스에서 실행 인스턴스가 아닌 곳으로 polling 이 라우팅돼도 진행 상태 조회.
        return ResponseEntity.ok(ApiResponse.success(progress.loadResponse()))
    }

    /**
     * 진행 상태 강제 초기화 — stale RUNNING(인스턴스 크래시/재시작으로 스냅샷이 RUNNING 으로 남아
     * UI 가 "실행 중" 으로 잠긴 상태) 해제용. progress 를 IDLE 로 되돌리고 Redis 스냅샷을 삭제한다.
     *
     * 안전 가드: 이 인스턴스가 실제 실행 중(in-memory RUNNING)이면 409 로 거부한다 — 진행 중인
     * 워커를 스냅샷만 지워 UI 에서 숨기는 오작동 방지. stale 상황은 "in-memory IDLE + Redis RUNNING"
     * 이므로 재시작된 인스턴스에서는 통과한다. 다중 인스턴스에서 다른 인스턴스가 실행 중일 가능성은
     * 운영자가 판단(화면 안내 문구).
     */
    @PostMapping("/api/v1/admin/sf-migration/stage1/reset")
    fun reset(): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        if (progress.status == Stage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        log.warn("[stage1-copy] 진행 상태 강제 초기화 (reset) — stale RUNNING 해제")
        progress.forceReset()
        return ResponseEntity.ok(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage1/targets")
    fun listTargets(): ResponseEntity<ApiResponse<List<Stage1Targets.TargetCsv>>> {
        return ResponseEntity.ok(ApiResponse.success(Stage1Targets.listWithCsv()))
    }

    /**
     * Stage1 적재 폼 기본값 — S3 bucket (운영 S3_BUCKET 환경 속성) + CSV 공통 prefix.
     * web 이 single/batch 폼을 프리필하고 사용자가 현재 사용할 bucket/경로를 확인하도록 노출.
     */
    @GetMapping("/api/v1/admin/sf-migration/stage1/defaults")
    fun getDefaults(): ResponseEntity<ApiResponse<Stage1Defaults>> {
        return ResponseEntity.ok(
            ApiResponse.success(Stage1Defaults(s3Bucket = configuredS3Bucket, s3KeyPrefix = DEFAULT_S3_KEY_PREFIX)),
        )
    }

    companion object {
        /** Stage1 CSV 의 S3 공통 경로 (bucket 하위). extract-csv.sh 의 input/ 업로드 관례와 정합. */
        const val DEFAULT_S3_KEY_PREFIX = "sf-migration/input"
    }
}

/**
 * Stage1 적재 폼 기본값 — UI 프리필 + 사용자 확인용.
 *
 * @param s3Bucket    운영 S3 bucket (S3_BUCKET 환경 속성). 미설정(local 등) 시 빈 문자열.
 * @param s3KeyPrefix CSV 공통 prefix (예: "sf-migration/input").
 */
data class Stage1Defaults(
    val s3Bucket: String,
    val s3KeyPrefix: String,
)
