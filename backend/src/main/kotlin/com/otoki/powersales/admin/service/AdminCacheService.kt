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
) {

    private val log = LoggerFactory.getLogger(AdminCacheService::class.java)

    /**
     * 모든 cache name + 추정 key 개수 반환. 운영도구 UI 의 표 source.
     *
     * key 개수는 Redis `SCAN` 으로 추정 — 큰 cache 의 `KEYS` 명령 부하 회피.
     * Redis 미사용 환경 (local NoOpCacheManager) 은 size = -1 (불명).
     */
    fun listCaches(): List<CacheInfo> {
        return cacheManager.cacheNames.sorted().map { name ->
            val size = estimateKeyCount(name)
            CacheInfo(name = name, estimatedKeyCount = size)
        }
    }

    /**
     * 단일 cache name 전체 evict. Spring `Cache.clear()` 호출.
     * Redis backend 면 `cacheName::*` 전체 키 삭제.
     */
    fun evict(cacheName: String, actorEmployeeCode: String): EvictResult {
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
