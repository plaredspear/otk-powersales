package com.otoki.powersales.admin.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

@DisplayName("AdminCacheService 테스트")
class AdminCacheServiceTest {

    // ConcurrentMapCacheManager 로 실제 cache 동작 검증 (RedisTemplate 은 null → estimateKeyCount = -1).
    private val cacheManager = ConcurrentMapCacheManager(
        "teamScheduleBranchesV2",
        "admin-permission:v1",
        "admin-data-scope:v1",
    )
    private val service = AdminCacheService(cacheManager, null)

    @Test
    @DisplayName("listCaches - CacheManager 등록 캐시 (권한 가드 캐시 포함) 정렬 반환")
    fun listCaches_includesAllManagedCaches() {
        val result = service.listCaches()

        assertThat(result.map { it.name }).containsExactly(
            "admin-data-scope:v1",
            "admin-permission:v1",
            "teamScheduleBranchesV2",
        )
        // non-Redis (ConcurrentMap) 이므로 추정 불가 = -1.
        assertThat(result).allMatch { it.estimatedKeyCount == -1L }
    }

    @Test
    @DisplayName("evict - 등록된 cache 의 Cache.clear 호출 (entry 제거됨)")
    fun evict_clearsCache() {
        cacheManager.getCache("admin-permission:v1")!!.put(1L, setOf("account:R"))
        assertThat(cacheManager.getCache("admin-permission:v1")!!.get(1L)).isNotNull()

        val result = service.evict("admin-permission:v1", "ADMIN001")

        assertThat(result.cacheName).isEqualTo("admin-permission:v1")
        assertThat(cacheManager.getCache("admin-permission:v1")!!.get(1L)).isNull()
    }

    @Test
    @DisplayName("evict - 미등록 cache name 은 예외")
    fun evict_unknownNameThrows() {
        // ConcurrentMapCacheManager 는 기본 dynamic=true 라 미등록 이름도 lazy 생성됨 →
        // setCacheNames 로 dynamic 비활성화하여 미등록 이름은 getCache=null 이 되도록 strict 검증.
        val strictManager = ConcurrentMapCacheManager().apply {
            setCacheNames(listOf("admin-permission:v1"))
        }
        val strictService = AdminCacheService(strictManager, null)

        assertThatThrownBy { strictService.evict("nonexistent-cache", "ADMIN001") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("evictAll - 모든 등록 cache 순회 evict")
    fun evictAll_iteratesAllCaches() {
        cacheManager.getCache("admin-permission:v1")!!.put(1L, setOf("account:R"))
        cacheManager.getCache("teamScheduleBranchesV2")!!.put("ALL", listOf("x"))

        val results = service.evictAll("ADMIN001")

        assertThat(results.map { it.cacheName }).containsExactlyInAnyOrder(
            "admin-data-scope:v1",
            "admin-permission:v1",
            "teamScheduleBranchesV2",
        )
        assertThat(cacheManager.getCache("admin-permission:v1")!!.get(1L)).isNull()
        assertThat(cacheManager.getCache("teamScheduleBranchesV2")!!.get("ALL")).isNull()
    }
}
