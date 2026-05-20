package com.otoki.powersales.sfmigration.stage1

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
 *  - POST /copy-all-from-s3   : 일괄 (등록된 모든 entity) 실행 — 의존성 순서, 1개 실패 시 즉시 중단
 */
// Stage1S3CopyService 와 동일하게 dev/prod 에서만 활성화 (S3Client 의존 + 운영 도구 성격).
@RestController
@Profile("dev | prod")
class Stage1CopyController(
    private val service: Stage1S3CopyService,
    private val progress: Stage1CopyProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sf-stage1-copy").apply { isDaemon = true }
    }

    @PostMapping("/api/v1/admin/sf-migration/stage1/copy-from-s3")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun copyFromS3(
        @Valid @RequestBody req: Stage1CopyRequest,
    ): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        if (progress.status == Stage1CopyProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(progress.toResponse()))
        }
        progress.begin(req.targetName, req.s3Bucket, req.s3Key)
        executor.submit {
            try {
                service.copyFromS3(req.targetName, req.s3Bucket, req.s3Key)
            } catch (e: Exception) {
                log.error("[stage1-copy] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @PostMapping("/api/v1/admin/sf-migration/stage1/copy-all-from-s3")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
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
                service.copyAllFromS3(req.s3Bucket, req.s3KeyPrefix)
            } catch (e: Exception) {
                log.error("[stage1-copy-all] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage1/copy-from-s3/progress")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun getProgress(): ResponseEntity<ApiResponse<Stage1CopyProgressResponse>> {
        return ResponseEntity.ok(ApiResponse.success(progress.toResponse()))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage1/targets")
    @RequiresPermission(AdminPermission.SF_MIGRATION_RUN)
    fun listTargets(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(Stage1Targets.list()))
    }

    private fun Stage1CopyProgress.toResponse(): Stage1CopyProgressResponse {
        return Stage1CopyProgressResponse(
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
            errors = errors.toList(),
            entityResults = entityResults.map {
                Stage1EntityResultResponse(
                    targetName = it.targetName,
                    status = it.status.name,
                    s3Key = it.s3Key,
                    processedRows = it.processedRows,
                    filteredOut = it.filteredOut,
                    insertedRows = it.insertedRows,
                    errorMessage = it.errorMessage,
                    startedAt = it.startedAt,
                    finishedAt = it.finishedAt,
                )
            },
        )
    }
}
