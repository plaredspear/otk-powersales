package com.otoki.powersales.herokumigration.stage1

import com.otoki.powersales.common.dto.ApiResponse
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
 * Heroku migration Stage 1 — S3 의 export CSV 를 backend 가 직접 PostgreSQL 에 COPY 적재.
 *
 * SF [com.otoki.powersales.sfmigration.stage1.Stage1CopyController] 와 동형.
 *
 * 권한: 로그인(authenticated)만 요구 — 마이그레이션이 권한 데이터(employee_info 등) 를 적재하는
 * 단계라 MODIFY_ALL_DATA 부트스트랩 닭-달걀 회피 (스펙 §1.0). web 사이드 메뉴 제외 + URL 직접 진입.
 * 완료 후 가드 복원 권장.
 *
 * 사전 조건:
 *  - 사용자가 TablePlus export CSV 를 S3 에 업로드해둔 상태.
 *  - EmployeeInfo 적재 전 SF 마이그레이션으로 employee 가 선행 적재되어 있어야 공유 PK resolve 성립.
 *
 * 비동기 실행 — 단일 thread executor 로 동시 1회만 enforce. RUNNING 중 신규 요청은 409.
 * 진행 상태는 GET /progress 로 polling.
 */
// HerokuStage1S3CopyService 와 동일하게 dev/prod 에서만 활성화 (S3Client 의존 + 운영 도구 성격).
@RestController
@Profile("dev | prod")
class HerokuStage1CopyController(
    private val service: HerokuStage1S3CopyService,
    private val progress: HerokuStage1CopyProgress,
    // 운영 S3 bucket (EB 콘솔 환경 속성 S3_BUCKET). UI 프리필.
    @Value("\${app.aws.s3.bucket:}") private val configuredS3Bucket: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "heroku-stage1-copy").apply { isDaemon = true }
    }

    @PostMapping("/api/v1/admin/heroku-migration/stage1/copy-from-s3")
    fun copyFromS3(
        @Valid @RequestBody req: HerokuStage1CopyRequest,
    ): ResponseEntity<ApiResponse<HerokuStage1CopyProgressResponse>> {
        if (progress.status == HerokuStage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        val meta = HerokuStage1Targets.get(req.targetName)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("UNKNOWN_TARGET", "Unknown target: ${req.targetName}"))
        val s3Key = "${req.s3KeyPrefix.removeSuffix("/")}/${meta.csvFileName}"
        progress.begin(req.targetName, req.s3Bucket, s3Key)
        executor.submit {
            try {
                service.copyFromS3(req.targetName, req.s3Bucket, s3Key, req.reset, req.maxRows)
            } catch (e: Exception) {
                log.error("[heroku-stage1-copy] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @PostMapping("/api/v1/admin/heroku-migration/stage1/copy-all-from-s3")
    fun copyAllFromS3(
        @Valid @RequestBody req: HerokuStage1CopyAllRequest,
    ): ResponseEntity<ApiResponse<HerokuStage1CopyProgressResponse>> {
        if (progress.status == HerokuStage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        progress.beginBatch(req.s3Bucket, HerokuStage1Targets.list())
        executor.submit {
            try {
                service.copyAllFromS3(req.s3Bucket, req.s3KeyPrefix, req.reset, req.maxRows)
            } catch (e: Exception) {
                log.error("[heroku-stage1-copy-all] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/heroku-migration/stage1/copy-from-s3/progress")
    fun getProgress(): ResponseEntity<ApiResponse<HerokuStage1CopyProgressResponse>> {
        return ResponseEntity.ok(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/heroku-migration/stage1/targets")
    fun listTargets(): ResponseEntity<ApiResponse<List<HerokuStage1Targets.TargetCsv>>> {
        return ResponseEntity.ok(ApiResponse.success(HerokuStage1Targets.listWithCsv()))
    }

    /**
     * Stage1 적재 폼 기본값 — S3 bucket (운영 S3_BUCKET 환경 속성) + CSV 공통 prefix.
     */
    @GetMapping("/api/v1/admin/heroku-migration/stage1/defaults")
    fun getDefaults(): ResponseEntity<ApiResponse<HerokuStage1Defaults>> {
        return ResponseEntity.ok(
            ApiResponse.success(
                HerokuStage1Defaults(s3Bucket = configuredS3Bucket, s3KeyPrefix = DEFAULT_S3_KEY_PREFIX),
            ),
        )
    }

    private fun HerokuStage1CopyProgress.toResponse(): HerokuStage1CopyProgressResponse {
        return HerokuStage1CopyProgressResponse(
            status = status.name,
            mode = mode.name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            targetName = targetName,
            s3Bucket = s3Bucket,
            s3Key = s3Key,
            processedRows = processedRows,
            filteredOut = filteredOut,
            insertedRows = insertedRows,
            unmatchedRows = unmatchedRows,
            errors = errors.toList(),
            entityResults = entityResults.map {
                HerokuStage1EntityResultResponse(
                    targetName = it.targetName,
                    status = it.status.name,
                    s3Key = it.s3Key,
                    processedRows = it.processedRows,
                    filteredOut = it.filteredOut,
                    insertedRows = it.insertedRows,
                    unmatchedRows = it.unmatchedRows,
                    errorMessage = it.errorMessage,
                    startedAt = it.startedAt,
                    finishedAt = it.finishedAt,
                )
            },
        )
    }

    companion object {
        /** Stage1 CSV 의 S3 공통 경로 (bucket 하위). */
        const val DEFAULT_S3_KEY_PREFIX = "heroku-migration/input"
    }
}
