package com.otoki.powersales._migration.common

import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales.admin.service.AdminCacheService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service

/**
 * 데이터 마이그레이션 컷오버용 Redis 정리 (1회성 도구).
 *
 * ## 왜 필요한가 — PK 재부여와 캐시 오염
 *
 * Redis 캐시 다수가 `employee.id` / 조직 PK 를 키로 쓴다 (`hierarchySubordinates`,
 * `memberGroupIds`, `profileFlags`, `permissionSetFlags`, `sharing-rules-for-user`,
 * `admin-permission`, `admin-data-scope`, `organizationCascadeV2` …). DB reset 후 재적재로 PK 가
 * 재부여되면 **이전 사원의 캐시가 새 사원에게 그대로 붙는다** — 권한/조직 범위가 섞이는 사고다.
 * TTL 이 있어도 만료 전까지는 오염된 응답이 나가므로 컷오버 시 명시적으로 비운다.
 *
 * ## 왜 FLUSHDB 가 아닌가
 *
 * 같은 Redis 에 **운영자가 손으로 바꿔 둔 런타임 토글**이 함께 산다 — 배치 on/off
 * (`scheduled-job:enabled:*`), SAP 인바운드 엔드포인트 토글 (`sap:inbound:enabled:*`),
 * 기능 차단 (`feature_toggle:*`), 지점 스코프 모드 (`branch_scope:mode`). FLUSHDB 는 이것들을
 * 전부 설정 기본값으로 되돌려, 꺼 둔 배치가 다시 켜지는 식의 조용한 동작 변경을 만든다.
 * 그래서 **지울 대상만 열거하는 화이트리스트** 방식으로 삭제한다.
 *
 * ## 지우지 않는 것과 그 이유
 *
 * - `blacklist:<sha256(token)>` — 키가 토큰 해시라 PK 재부여와 무관하고 TTL(≤ access TTL) 로 자동 소멸.
 * - `migration:progress:*` — **본 작업 자신의 진행 상태**([MigrationProgressStore]). 지우면 실행 중인
 *   마이그레이션 화면이 IDLE 로 되돌아간다.
 * - 위 런타임 토글 4종.
 *
 * 세션 무효화는 본 서비스의 책임이 아니다 — 잔존 토큰은 발급시각 컷오프
 * (`jwt.min-issued-at`, [com.otoki.powersales.platform.common.security.JwtTokenProvider]) 가 끊는다.
 * 여기서 `active_device:*` 를 지우는 것은 세션 차단 목적이 아니라, PK 재부여 시 **다른 사원의 단말
 * UUID 가 캐시에 남아 오판정**되는 것을 막기 위해서다 (SoT 는 DB `employee_info.device_uuid`).
 */
@Service
class MigrationRedisResetService(
    private val adminCacheService: AdminCacheService,
    /** Redis 미사용 환경(local NoOp / test profile)에서는 빈 미등록 — null 이면 패턴 삭제를 건너뛴다. */
    private val redisTemplate: RedisTemplate<String, String>?,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Spring Cache 전량 evict + PK 키 기반 Redis 키 삭제.
     *
     * 삭제 건수를 substep 결과로 돌려주므로, 운영자가 화면에서 "실제로 뭐가 몇 개 지워졌는지" 를
     * 확인할 수 있다. 0 건이어도 정상이다 (이미 비어 있거나 앱이 아직 캐시를 채우지 않은 상태).
     *
     * **마이그레이션이 끝난 뒤에 실행할 것.** 적재 중 실행하면 살아 있는 앱 트래픽이 구 데이터로
     * 캐시를 다시 채운다.
     */
    fun run(actorEmployeeCode: String): SfMigrationStage2Response {
        val cacheResults = adminCacheService.evictAll(actorEmployeeCode).map { result ->
            SubstepResult(
                label = "cache: ${result.cacheName}",
                // keysBefore = -1 은 개수 추정 불가(NoOpCache/Redis 미사용) — 건수로는 0 으로 집계한다.
                rowsAffected = result.keysBefore.coerceAtLeast(0L).toInt(),
            )
        }

        val patternResults = PK_KEYED_PATTERNS.map { pattern ->
            SubstepResult(label = "key: $pattern", rowsAffected = deleteByPattern(pattern))
        }

        val results = cacheResults + patternResults
        val total = results.sumOf { it.rowsAffected }
        log.info(
            "[MIGRATION_REDIS_RESET] actor={} caches={} patterns={} totalKeys={}",
            actorEmployeeCode, cacheResults.size, patternResults.size, total
        )
        return SfMigrationStage2Response(
            substep = SUBSTEP,
            results = results,
            totalRowsAffected = total,
        )
    }

    /**
     * `SCAN` 으로 패턴에 매칭되는 키를 모아 삭제하고 삭제 건수를 반환.
     *
     * `KEYS` 는 단일 스레드 Redis 를 패턴 매칭 동안 통째로 블로킹하므로 쓰지 않는다
     * ([AdminCacheService] 의 키 개수 추정과 동일한 판단).
     */
    private fun deleteByPattern(pattern: String): Int {
        val template = redisTemplate ?: return 0
        return try {
            val keys = template.execute(
                RedisCallback { conn ->
                    val found = mutableListOf<String>()
                    val options = ScanOptions.scanOptions().match(pattern).count(1000).build()
                    conn.keyCommands().scan(options).use { cursor ->
                        while (cursor.hasNext()) {
                            found += String(cursor.next())
                        }
                    }
                    found
                }
            ).orEmpty()
            if (keys.isEmpty()) 0 else (template.delete(keys) ?: 0L).toInt()
        } catch (e: Exception) {
            // 한 패턴의 실패가 나머지 정리를 막지 않도록 삼킨다 (best-effort). 결과는 0 건으로 보고된다.
            log.error("[MIGRATION_REDIS_RESET] 패턴 삭제 실패: pattern={} cause={}", pattern, e.message)
            0
        }
    }

    companion object {
        const val SUBSTEP: String = "redis-reset"

        /**
         * PK(employee.id 등)를 키에 담고 있어 재부여 시 오염되는 키 패턴.
         *
         * Spring Cache(`<cacheName>::*`)는 [AdminCacheService.evictAll] 이 담당하므로 여기엔 없다.
         * 새로 Redis 를 직접 쓰는 곳이 생기면 **키에 PK 가 들어가는지** 확인해 여기 추가한다.
         */
        val PK_KEYED_PATTERNS: List<String> = listOf(
            // 활성 단말 캐시 — key = active_device:<employeeId> (ActiveDeviceStore)
            "active_device:*",
            // 진열 일정 업로드 staging — 적재 전 임시 데이터라 DB reset 후에는 의미가 없다
            "schedule:upload:*",
        )
    }
}
