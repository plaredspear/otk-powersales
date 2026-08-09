package com.otoki.powersales._migration.common

import com.otoki.powersales.admin.service.AdminCacheService
import com.otoki.powersales.external.sap.inbound.toggle.SapInboundToggleStore
import com.otoki.powersales.platform.batch.toggle.ScheduledJobToggleStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate

@DisplayName("MigrationRedisResetService — 마이그레이션 컷오버 Redis 정리")
class MigrationRedisResetServiceTest {

    private val adminCacheService = mockk<AdminCacheService>()
    private val redisTemplate = mockk<RedisTemplate<String, String>>()

    private fun service(template: RedisTemplate<String, String>? = redisTemplate) =
        MigrationRedisResetService(adminCacheService, template)

    @Nested
    @DisplayName("run - 캐시 evict + PK 키 삭제")
    inner class RunTests {

        @Test
        @DisplayName("Spring Cache evict 건수와 패턴 삭제 건수를 substep 결과로 합산한다")
        fun aggregatesCacheAndPatternResults() {
            // Given: 캐시 2종 (7건 + 3건) + 패턴별 2건씩 삭제
            every { adminCacheService.evictAll(any()) } returns listOf(
                AdminCacheService.EvictResult("profileFlags", keysBefore = 7, keysAfter = 0),
                AdminCacheService.EvictResult("memberGroupIds", keysBefore = 3, keysAfter = 0),
            )
            every { redisTemplate.execute(any<RedisCallback<MutableList<String>>>()) } returns
                mutableListOf("k1", "k2")
            every { redisTemplate.delete(any<Collection<String>>()) } returns 2L

            // When
            val response = service().run("20230001")

            // Then: 캐시 10건 + 패턴 2종 × 2건 = 14건
            assertThat(response.substep).isEqualTo("redis-reset")
            assertThat(response.totalRowsAffected).isEqualTo(14)
            assertThat(response.results.map { it.label }).containsExactly(
                "cache: profileFlags",
                "cache: memberGroupIds",
                "key: active_device:*",
                "key: schedule:upload:*",
            )
            verify { adminCacheService.evictAll("20230001") }
        }

        @Test
        @DisplayName("삭제 대상 키가 없으면 0 건으로 보고한다 (재실행 안전 — 멱등)")
        fun reportsZeroWhenNothingMatches() {
            // Given
            every { adminCacheService.evictAll(any()) } returns emptyList()
            every { redisTemplate.execute(any<RedisCallback<MutableList<String>>>()) } returns
                mutableListOf()

            // When
            val response = service().run("migration")

            // Then: 키가 없으면 DELETE 자체를 보내지 않는다
            assertThat(response.totalRowsAffected).isEqualTo(0)
            verify(exactly = 0) { redisTemplate.delete(any<Collection<String>>()) }
        }

        @Test
        @DisplayName("Redis 미사용 환경(RedisTemplate 미등록)에서도 캐시 evict 는 수행하고 실패하지 않는다")
        fun worksWithoutRedisTemplate() {
            // Given: local NoOp / test profile — RedisTemplate 빈이 없다
            every { adminCacheService.evictAll(any()) } returns listOf(
                // 개수 추정 불가(-1)는 음수로 새어 나가지 않고 0 으로 집계돼야 한다
                AdminCacheService.EvictResult("profileFlags", keysBefore = -1, keysAfter = -1),
            )

            // When
            val response = service(template = null).run("migration")

            // Then
            assertThat(response.totalRowsAffected).isEqualTo(0)
            assertThat(response.results).hasSize(1 + MigrationRedisResetService.PK_KEYED_PATTERNS.size)
        }

        @Test
        @DisplayName("한 패턴의 Redis 실패가 나머지 정리를 막지 않는다 (best-effort)")
        fun oneFailingPatternDoesNotAbortTheRest() {
            // Given: SCAN 이 1회 실패 후 성공
            every { adminCacheService.evictAll(any()) } returns emptyList()
            every { redisTemplate.execute(any<RedisCallback<MutableList<String>>>()) }
                .throws(RuntimeException("redis down")) andThen mutableListOf("k1")
            every { redisTemplate.delete(any<Collection<String>>()) } returns 1L

            // When
            val response = service().run("migration")

            // Then: 실패 패턴은 0 건으로 보고되고 뒤 패턴은 정상 처리된다
            assertThat(response.results.map { it.rowsAffected }).containsExactly(0, 1)
            assertThat(response.totalRowsAffected).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("삭제 대상 패턴 — 운영 토글을 지우면 안 된다")
    inner class PatternSafetyTests {

        /**
         * FLUSHDB 를 쓰지 않는 이유가 곧 이 테스트다. 삭제 패턴에 와일드카드가 과하게 들어가면
         * 운영자가 손으로 꺼 둔 배치/기능 토글이 조용히 기본값으로 되살아난다.
         */
        @Test
        @DisplayName("런타임 토글 키는 어떤 삭제 패턴에도 매칭되지 않는다")
        fun preservesRuntimeToggleKeys() {
            val preserved = listOf(
                "${ScheduledJobToggleStore.KEY_PREFIX}claimMasterSync",
                "${SapInboundToggleStore.KEY_PREFIX}/api/v1/sap/employee-master",
                // FeatureToggleStore / BranchScopeModeStore 는 키 상수가 private 이라 리터럴로 둔다
                "feature_toggle:disabled:ORDER_FORM",
                "branch_scope:mode",
                // 실행 중인 마이그레이션 진행 상태 — 지우면 화면이 IDLE 로 되돌아간다
                "${MigrationProgressStore.KEY_PREFIX}heroku-stage2-fk",
                // 토큰 해시 키라 PK 재부여와 무관 + TTL 자동 소멸
                "blacklist:abc123",
            )

            val regexes = MigrationRedisResetService.PK_KEYED_PATTERNS.map { pattern ->
                Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) })
            }

            assertThat(preserved.filter { key -> regexes.any { it.matches(key) } }).isEmpty()
        }

        @Test
        @DisplayName("PK 키는 삭제 패턴에 매칭된다")
        fun matchesPkKeyedKeys() {
            val regexes = MigrationRedisResetService.PK_KEYED_PATTERNS.map { pattern ->
                Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) })
            }
            val targets = listOf("active_device:1234", "schedule:upload:5678")

            assertThat(targets.all { key -> regexes.any { it.matches(key) } }).isTrue()
        }
    }
}
