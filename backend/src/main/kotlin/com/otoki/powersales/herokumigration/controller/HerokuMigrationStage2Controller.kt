package com.otoki.powersales.herokumigration.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.herokumigration.service.HerokuFkResolveProgress
import com.otoki.powersales.herokumigration.service.HerokuFkResolveService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

/**
 * Heroku 데이터 마이그레이션 Stage 2 admin 엔드포인트 (1회성 cut-over).
 *
 * SF [com.otoki.powersales.sfmigration.controller.SfMigrationStage2Controller] 와 동형.
 *
 * 권한: 로그인(authenticated)만 요구 — 마이그레이션이 권한 데이터를 적재하는 단계라 MODIFY_ALL_DATA
 * 부트스트랩 닭-달걀 회피 (스펙 §1.0). web 사이드 메뉴 제외 + URL 직접 진입. 완료 후 가드 복원 권장.
 *
 * 패턴 A(자연 키→serial id) + 패턴 B(부모 FK) 를 fk substep 으로 일괄 처리. 비동기 실행 +
 * 진행 상태 polling. 패턴 C(sfid) 는 SF 프레임워크 `POST /sf-migration/stage2/fk` 재사용 (별도 없음).
 */
@RestController
class HerokuMigrationStage2Controller(
    private val fkService: HerokuFkResolveService,
    private val fkProgress: HerokuFkResolveProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1회성 도구라 single-thread executor (동시 1회 실행 enforce).
    private val fkExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "heroku-fk-resolve").apply { isDaemon = true }
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
