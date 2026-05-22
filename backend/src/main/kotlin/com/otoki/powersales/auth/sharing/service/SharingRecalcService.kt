package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.entity.SharingRecalcLog
import com.otoki.powersales.auth.sharing.repository.SharingRecalcLogRepository
import com.otoki.powersales.common.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/**
 * Sharing Recalc service (spec #792).
 *
 * ## 결정 사항 정합
 * - Q1 옵션 1: Redis @Cacheable + Spring Cache abstraction — 다중 인스턴스 일관성
 * - Q2 옵션 1: Spring Cache `getNativeCache` stats (단순 구현)
 * - Q3 옵션 1: cache name 별 일괄 evict (SCAN/UNLINK 패턴은 Spring CacheManager 가 내부 처리)
 * - Q4 옵션 1: 동기 응답 — cache evict 만이라 즉시 완료
 *
 * ## 동작 비교 — SF vs 본 spec
 * | 차원 | SF Sharing Recalculation | 본 spec |
 * |---|---|---|
 * | 처리 방식 | background job (수 시간 ~ 며칠) | 동기 cache evict (ms 단위) |
 * | 데이터 재계산 | SharingRecord 테이블 row 재생성 | 데이터 재계산 없음 — 다음 read 시 evaluator 가 최신 메타로 predicate 재합성 |
 */
@Service
class SharingRecalcService(
    private val cacheManager: CacheManager,
    private val sharingRecalcLogRepository: SharingRecalcLogRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 전체 sharing 관련 cache 일괄 evict.
     *
     * @return evict 된 cache name 개수 + 처리 소요 ms
     */
    fun recalcAll(triggeredByUserId: Long): RecalcResult {
        val start = System.currentTimeMillis()
        var evictedCount = 0
        CacheConfig.SHARING_RELATED_CACHE_NAMES.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.let {
                it.clear()
                evictedCount++
                log.debug("[sharing-recalc] cache cleared — {}", cacheName)
            }
        }
        val duration = (System.currentTimeMillis() - start).toInt()

        // audit log
        sharingRecalcLogRepository.save(
            SharingRecalcLog(
                triggeredAt = OffsetDateTime.now(),
                triggeredByUserId = triggeredByUserId,
                scope = "ALL",
                evictedCacheCount = evictedCount,
                durationMs = duration,
            ),
        )

        log.info("[sharing-recalc] all complete — {} cache evicted in {} ms", evictedCount, duration)
        return RecalcResult(evictedCount, duration)
    }

    /**
     * 특정 sObject 한정 cache evict.
     *
     * 본 구현은 sharing 관련 cache 전체 clear (Q3 옵션 1 — key prefix 패턴은 Spring CacheManager 의
     * RedisCache 의 clear 호출이 cache name prefix scan + unlink 처리). sObject 별 fine-grained
     * eviction 은 cache key 패턴이 sObject 포함 시 별도 SCAN 필요 — 본 구현은 cache name 단위 일괄.
     */
    fun recalcSObject(sObjectName: String, triggeredByUserId: Long): RecalcResult {
        val start = System.currentTimeMillis()

        // sObject 별 fine-grained evict 는 sObject 가 cache key 의 일부인 cache name 만 영향 받음.
        // 본 구현은 단순화 — sharing 관련 cache 전체 clear (정합 안전 + 운영 단순).
        var evictedCount = 0
        CacheConfig.SHARING_RELATED_CACHE_NAMES.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.let {
                it.clear()
                evictedCount++
            }
        }
        val duration = (System.currentTimeMillis() - start).toInt()

        sharingRecalcLogRepository.save(
            SharingRecalcLog(
                triggeredAt = OffsetDateTime.now(),
                triggeredByUserId = triggeredByUserId,
                scope = "SOBJECT",
                sObjectName = sObjectName,
                evictedCacheCount = evictedCount,
                durationMs = duration,
            ),
        )

        log.info("[sharing-recalc] sObject={} complete — {} cache evicted in {} ms", sObjectName, evictedCount, duration)
        return RecalcResult(evictedCount, duration)
    }

    fun getStatus(): RecalcStatus {
        val last = sharingRecalcLogRepository.findTopByOrderByTriggeredAtDesc()
        return RecalcStatus(
            lastRecalcAt = last?.triggeredAt,
            lastRecalcScope = last?.scope,
            lastRecalcSObjectName = last?.sObjectName,
            lastRecalcTriggeredByUserId = last?.triggeredByUserId,
            lastEvictedCount = last?.evictedCacheCount,
            lastDurationMs = last?.durationMs,
        )
    }

    data class RecalcResult(val evictedCount: Int, val durationMs: Int)

    data class RecalcStatus(
        val lastRecalcAt: OffsetDateTime?,
        val lastRecalcScope: String?,
        val lastRecalcSObjectName: String?,
        val lastRecalcTriggeredByUserId: Long?,
        val lastEvictedCount: Int?,
        val lastDurationMs: Int?,
    )
}
