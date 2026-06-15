package com.otoki.powersales._migration.heroku.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales._migration.heroku.service.HerokuFkResolveProgress
import com.otoki.powersales._migration.heroku.service.HerokuFkResolveService
import com.otoki.powersales._migration.sf.dto.SfFkResolveProgressResponse
import com.otoki.powersales._migration.sf.service.SfFkResolveProgress
import com.otoki.powersales._migration.sf.service.SfMigrationStage2FkService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

/**
 * Heroku 데이터 마이그레이션 Stage 2 admin 엔드포인트 (1회성 cut-over).
 *
 * SF [com.otoki.powersales._migration.sf.controller.SfMigrationStage2Controller] 와 동형.
 *
 * 권한: 로그인(authenticated)만 요구 — 마이그레이션이 권한 데이터를 적재하는 단계라 MODIFY_ALL_DATA
 * 부트스트랩 닭-달걀 회피 (스펙 §1.0). web 사이드 메뉴 제외 + URL 직접 진입. 완료 후 가드 복원 권장.
 *
 * 패턴 A(자연 키→serial id) + 패턴 B(부모 FK) 를 fk substep 으로 일괄 처리. 비동기 실행 +
 * 진행 상태 polling.
 *
 * 패턴 C(sfid) 중 `@HerokuOnly` 이지만 `_sfid` 값이 진짜 SF Id 인 테이블
 * ([com.otoki.powersales._migration.sf.service.HEROKU_TABLES_WITH_SF_SFID] —
 * safety_check_submission / product_expiration) 의 FK Resolve 도 본 페이지에서 노출/실행한다.
 * 다만 sfid resolve 엔진은 SF [SfMigrationStage2FkService] 한 곳에만 있으므로, 본 컨트롤러는
 * UI 진입점만 제공하고 실행은 SF 서비스 + SF [SfFkResolveProgress] 를 재사용한다 (chunk/polymorphic
 * 로직 중복 회피). 그 외 일반 sfid 테이블은 SF 페이지 `POST /sf-migration/stage2/fk` 가 처리.
 */
@RestController
class HerokuMigrationStage2Controller(
    private val fkService: HerokuFkResolveService,
    private val fkProgress: HerokuFkResolveProgress,
    private val sfFkService: SfMigrationStage2FkService,
    private val sfFkProgress: SfFkResolveProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val fkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "heroku-fk-resolve").apply { isDaemon = true }
    }

    // sfid resolve 는 SF 엔진을 재사용하므로 SF 와 동일하게 별도 single-thread executor 로 직렬화.
    private val sfidFkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "heroku-sfid-fk-resolve").apply { isDaemon = true }
    }

    @PostMapping("/api/v1/admin/heroku-migration/stage2/fk")
    fun runFkResolve(): ResponseEntity<ApiResponse<HerokuFkResolveProgressResponse>> {
        if (fkProgress.status == HerokuFkResolveProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(fkProgress.toResponse()))
        }
        fkExecutor.submit {
            try {
                fkService.runFkResolve()
            } catch (e: Exception) {
                log.error("[heroku-fk] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(fkProgress.toResponse()))
    }

    @GetMapping("/api/v1/admin/heroku-migration/stage2/fk/progress")
    fun getFkProgress(): ResponseEntity<ApiResponse<HerokuFkResolveProgressResponse>> {
        return ResponseEntity.ok(ApiResponse.success(fkProgress.toResponse()))
    }

    /**
     * sfid FK Resolve 대상 테이블 목록 (Heroku 전용 sfid 테이블 드롭다운용).
     * [com.otoki.powersales._migration.sf.service.HEROKU_TABLES_WITH_SF_SFID] 중 실제 처리 계획이
     * 도출된 테이블만 정렬해 반환.
     */
    @GetMapping("/api/v1/admin/heroku-migration/stage2/sfid-fk/tables")
    fun getSfidFkResolvableTables(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(sfFkService.listHerokuSfidResolvableTables()))
    }

    /**
     * Heroku 전용 sfid 테이블의 FK Resolve 실행. 실행 엔진은 SF [SfMigrationStage2FkService] 재사용.
     *
     * @param tableName null/미지정 시 Heroku sfid 대상 전체, 지정 시 해당 테이블 1개만 처리.
     *   처리 가능한 테이블은 위 `.../sfid-fk/tables` 로 조회. SF FK Resolve 와 동일한 SF progress
     *   ([SfFkResolveProgress]) 를 공유하므로, SF 페이지/본 페이지 중 한쪽이 실행 중이면 중복 차단된다.
     */
    @PostMapping("/api/v1/admin/heroku-migration/stage2/sfid-fk")
    fun runSfidFkResolve(
        @RequestParam(name = "tableName", required = false) tableName: String?,
    ): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        if (sfFkProgress.status == SfFkResolveProgress.Status.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.success(sfFkProgress.toResponse()))
        }
        val requested = tableName?.takeIf { it.isNotBlank() }
        // 임의 테이블 실행 차단 — Heroku sfid 대상 (HEROKU_TABLES_WITH_SF_SFID) 으로만 한정.
        if (requested != null && requested !in sfFkService.listHerokuSfidResolvableTables()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.success(sfFkProgress.toResponse()))
        }
        sfidFkExecutor.submit {
            try {
                val targets = requested?.let { setOf(it) }
                    ?: sfFkService.listHerokuSfidResolvableTables().toSet()
                // Heroku sfid 대상만 한 progress 로 일괄 처리 (SF 일반 테이블은 건드리지 않음).
                sfFkService.runFkResolveForTables(targets)
            } catch (e: Exception) {
                log.error("[heroku-sfid-fk] async run failed", e)
            }
        }
        return ResponseEntity.accepted().body(ApiResponse.success(sfFkProgress.toResponse()))
    }

    @GetMapping("/api/v1/admin/heroku-migration/stage2/sfid-fk/progress")
    fun getSfidFkProgress(): ResponseEntity<ApiResponse<SfFkResolveProgressResponse>> {
        return ResponseEntity.ok(ApiResponse.success(sfFkProgress.toResponse()))
    }

    /** SF progress → 응답 변환 (SF 컨트롤러와 동일 매핑 — sfid resolve 는 SF progress 공유). */
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

    private fun HerokuFkResolveProgress.toResponse(): HerokuFkResolveProgressResponse {
        return HerokuFkResolveProgressResponse(
            status = status.name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalTables = totalTables,
            completedTables = completedTablesCount,
            currentTable = currentTable,
            totalRowsAffected = totalRowsAffected,
            tableResults = tableResults.map {
                HerokuFkTableResult(table = it.table, column = it.column, rowsAffected = it.rowsAffected)
            },
            unmatched = unmatched.map {
                HerokuFkUnmatched(
                    table = it.table,
                    column = it.column,
                    naturalKey = it.naturalKey,
                    unmatchedCount = it.unmatchedCount,
                )
            },
            errors = errors.toList(),
        )
    }
}
