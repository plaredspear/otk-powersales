package com.otoki.powersales.admin.service

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service

/**
 * 운영도구 - Redis 캐시 evict 서비스.
 *
 * 배경: fix 배포 직후 [@Cacheable] 결과가 stale 상태로 남아 24h TTL 또는 SAP daily sync 까지
 * 갱신되지 않는 사례 (예: `findTeamScheduleBranches` 의 `costCenterLevel*` → `orgCodeLevel*` fix 이후
 * dev 의 stale `branches: []` entry) 가 반복되어, 운영자가 web UI 에서 직접 cache name 단위로
 * evict 할 수 있도록 한다.
 *
 * 그레늄리티: cache name 전체 (Spring `Cache.clear()`). key-pattern 부분 evict 는 운영 실수 위험이 커
 * 본 spec 단계에서는 지원하지 않는다.
 */
@Service
class AdminCacheService(
    private val cacheManager: CacheManager,
    /**
     * Redis 미사용 환경 (test profile 등) 에서는 빈 미등록 — null 허용.
     * 본 클래스가 RedisTemplate 을 keyCommands().scan() 카운팅 용도로만 사용해 generic 타입은 무관.
     */
    private val redisTemplate: RedisTemplate<String, String>?,
    /**
     * CacheManager 미등록 in-memory(Caffeine) 권한 캐시 (adminPermissionCache / adminDataScopeCache).
     * 가상 cache name (`mem:*`) 으로 list / evict 에 합류.
     */
    private val inMemoryPermissionCacheRegistry: InMemoryPermissionCacheRegistry,
) {

    private val log = LoggerFactory.getLogger(AdminCacheService::class.java)

    /**
     * 모든 cache name + 추정 key 개수 반환. 운영도구 UI 의 표 source.
     *
     * key 개수는 Redis `SCAN` 으로 추정 — 큰 cache 의 `KEYS` 명령 부하 회피.
     * Redis 미사용 환경 (local NoOpCacheManager) 은 size = -1 (불명).
     *
     * Spring CacheManager 캐시 + in-memory 권한 캐시 (가상 cache name `mem:*`) 합집합.
     */
    fun listCaches(): List<CacheInfo> {
        val managed = cacheManager.cacheNames.sorted().map { name ->
            CacheInfo(name = name, estimatedKeyCount = estimateKeyCount(name))
        }
        val inMemory = inMemoryPermissionCacheRegistry.list().map { (name, size) ->
            CacheInfo(name = name, estimatedKeyCount = size)
        }
        return managed + inMemory
    }

    /**
     * 단일 cache name 전체 evict.
     * - 가상 cache name (`mem:*`): [InMemoryPermissionCacheRegistry] 의 Caffeine `invalidateAll()`.
     * - 그 외: Spring `Cache.clear()` (Redis backend 면 `cacheName::*` 전체 키 삭제).
     */
    fun evict(cacheName: String, actorEmployeeCode: String): EvictResult {
        if (inMemoryPermissionCacheRegistry.handles(cacheName)) {
            val (before, after) = inMemoryPermissionCacheRegistry.evict(cacheName)
            log.info(
                "[ADMIN_CACHE_EVICT] actor={} cacheName={} (in-memory) keysBefore={} keysAfter={}",
                actorEmployeeCode, cacheName, before, after
            )
            return EvictResult(cacheName = cacheName, keysBefore = before, keysAfter = after)
        }
        val cache = cacheManager.getCache(cacheName)
            ?: throw IllegalArgumentException("존재하지 않는 cache name: $cacheName")
        val before = estimateKeyCount(cacheName)
        cache.clear()
        val after = estimateKeyCount(cacheName)
        log.info(
            "[ADMIN_CACHE_EVICT] actor={} cacheName={} keysBefore={} keysAfter={}",
            actorEmployeeCode, cacheName, before, after
        )
        return EvictResult(cacheName = cacheName, keysBefore = before, keysAfter = after)
    }

    /**
     * 등록된 모든 cache (Spring CacheManager + in-memory 권한 캐시) 일괄 evict.
     *
     * 운영자가 "fix 배포 직후 어느 캐시가 stale 인지 특정이 어려운" 상황에서 전체를 한 번에 비울 때 사용.
     * cache name 별 [evict] 를 순회 호출하며, 1개 실패 시 나머지는 계속 진행 (best-effort).
     *
     * @return cache name 별 [EvictResult] 일람 (실패 cache 는 keysAfter = -1 + 로그).
     */
    fun evictAll(actorEmployeeCode: String): List<EvictResult> {
        val allNames = listCaches().map { it.name }
        log.info("[ADMIN_CACHE_EVICT_ALL] actor={} totalCaches={}", actorEmployeeCode, allNames.size)
        val results = allNames.map { name ->
            try {
                evict(name, actorEmployeeCode)
            } catch (e: Exception) {
                log.error("[ADMIN_CACHE_EVICT_ALL] cacheName={} evict 실패: {}", name, e.message)
                EvictResult(cacheName = name, keysBefore = -1L, keysAfter = -1L)
            }
        }
        log.info("[ADMIN_CACHE_EVICT_ALL] actor={} done — {} caches cleared", actorEmployeeCode, results.size)
        return results
    }

    private fun estimateKeyCount(cacheName: String): Long {
        val cache = cacheManager.getCache(cacheName) ?: return -1L
        // RedisCache 외 (NoOpCache 등) 는 추정 불가 — UI 가 "-" 로 표시.
        if (cache !is RedisCache) return -1L
        val template = redisTemplate ?: return -1L
        // SCAN MATCH "<cacheName>::*" — `RedisCache` 의 실제 key prefix 와 일치. cursor 순회로 count 만 집계.
        val pattern = "$cacheName::*"
        return template.execute(RedisCallback { conn ->
            var count = 0L
            val options = ScanOptions.scanOptions().match(pattern).count(1000).build()
            conn.keyCommands().scan(options).use { cursor ->
                while (cursor.hasNext()) {
                    cursor.next()
                    count++
                }
            }
            count
        }) ?: -1L
    }

    data class CacheInfo(
        val name: String,
        /** Redis SCAN 으로 추정한 key 개수. -1 = 미지원 (NoOpCache 등). */
        val estimatedKeyCount: Long,
    )

    data class EvictResult(
        val cacheName: String,
        val keysBefore: Long,
        val keysAfter: Long,
    )
}
