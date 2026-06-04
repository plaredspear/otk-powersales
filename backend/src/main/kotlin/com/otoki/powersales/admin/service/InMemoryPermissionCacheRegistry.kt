package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.auth.permission.AdminPermissionCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * CacheManager 미등록 in-memory(Caffeine) 권한 캐시들을 가상 cache name 으로 묶어
 * "Redis 캐시 관리" UI evict + SF 마이그레이션 완료 후 일괄 무효화에서 공통 참조하는 registry.
 *
 * ## 배경
 *
 * [AdminPermissionCache] / [AdminDataScopeCache] 는 각자 `Caffeine.newBuilder()` 자체 인스턴스라
 * Spring [org.springframework.cache.CacheManager] 에 등록되지 않는다. 따라서:
 *  - "Redis 캐시 관리" 페이지의 [AdminCacheService.evict] (= `cacheManager.getCache().clear()`) 로
 *    비워지지 않는다 — 운영자가 "캐시 다 비웠는데 권한이 안 바뀐다" 를 겪는 원인.
 *  - SF 데이터 마이그레이션이 `profile_flags` / `permission_set_assignment` 등 권한 원천 테이블을
 *    직접 적재해도 이 캐시는 5분 TTL 만료 전까지 stale snapshot 을 유지한다 (마이그레이션 경로에
 *    명시 invalidate 가 없었음).
 *
 * 본 registry 는 두 캐시를 cacheManager 로 이주시키지 않고 (get(principal) 커스텀 loader / DTO 직렬화
 * 재설계 회피), 가상 cache name 단위로 list / evict 만 공통 제공한다.
 *
 * ## 가상 cache name
 *
 * Spring CacheManager 의 실제 cache name 과 충돌하지 않도록 `mem:` prefix 를 붙인다.
 *  - `mem:adminPermissionCache` → [AdminPermissionCache]
 *  - `mem:adminDataScopeCache`  → [AdminDataScopeCache]
 */
@Component
class InMemoryPermissionCacheRegistry(
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Entry(
        val name: String,
        val estimatedSize: () -> Long,
        val invalidateAll: () -> Unit,
    )

    private val entries: List<Entry> = listOf(
        Entry(
            name = NAME_ADMIN_PERMISSION_CACHE,
            estimatedSize = adminPermissionCache::estimatedSize,
            invalidateAll = adminPermissionCache::invalidateAll,
        ),
        Entry(
            name = NAME_ADMIN_DATA_SCOPE_CACHE,
            estimatedSize = adminDataScopeCache::estimatedSize,
            invalidateAll = adminDataScopeCache::invalidateAll,
        ),
    )

    /** 가상 cache name → 추정 entry 개수. "Redis 캐시 관리" 표 source 합류용. */
    fun list(): List<Pair<String, Long>> = entries.map { it.name to it.estimatedSize() }

    /** 본 registry 가 관리하는 가상 cache name 인지. */
    fun handles(cacheName: String): Boolean = entries.any { it.name == cacheName }

    /**
     * 단일 가상 cache name 전체 무효화. before/after 추정 개수 반환 (UI 표시용).
     * @throws IllegalArgumentException 미등록 가상 cache name.
     */
    fun evict(cacheName: String): Pair<Long, Long> {
        val entry = entries.firstOrNull { it.name == cacheName }
            ?: throw IllegalArgumentException("존재하지 않는 in-memory cache name: $cacheName")
        val before = entry.estimatedSize()
        entry.invalidateAll()
        val after = entry.estimatedSize()
        return before to after
    }

    /**
     * 모든 in-memory 권한 캐시 일괄 무효화 — SF 마이그레이션 완료 후 호출.
     *
     * 주의: 본 호출은 호출된 단일 JVM 인스턴스의 Caffeine 만 비운다. 멀티 인스턴스 dev/prod 에서는
     * 각 인스턴스가 자체 5분 TTL 로 자연 회복하거나, 인스턴스별로 "Redis 캐시 관리" evict 가 필요하다.
     */
    fun invalidateAll() {
        entries.forEach { it.invalidateAll() }
        log.info("[in-memory-permission-cache] invalidateAll — {} caches cleared (this instance only)", entries.size)
    }

    companion object {
        const val NAME_ADMIN_PERMISSION_CACHE = "mem:adminPermissionCache"
        const val NAME_ADMIN_DATA_SCOPE_CACHE = "mem:adminDataScopeCache"
    }
}
