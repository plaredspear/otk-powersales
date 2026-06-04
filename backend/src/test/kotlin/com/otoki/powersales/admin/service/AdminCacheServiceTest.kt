package com.otoki.powersales.admin.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

@DisplayName("AdminCacheService 테스트")
class AdminCacheServiceTest {

    private val cacheManager = mockk<CacheManager>(relaxed = true)
    private val registry = mockk<InMemoryPermissionCacheRegistry>(relaxed = true)

    // RedisTemplate 은 null (non-Redis 환경 시뮬레이션 — estimateKeyCount = -1).
    private val service = AdminCacheService(cacheManager, null, registry)

    @Test
    @DisplayName("listCaches - CacheManager 캐시 + in-memory 가상 캐시 합집합 반환")
    fun listCaches_mergesManagedAndInMemory() {
        every { cacheManager.cacheNames } returns setOf("teamScheduleBranchesV2", "organizationCascadeV2")
        every { cacheManager.getCache(any()) } returns mockk<Cache>(relaxed = true)
        every { registry.list() } returns listOf(
            InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE to 4L,
            InMemoryPermissionCacheRegistry.NAME_ADMIN_DATA_SCOPE_CACHE to 2L,
        )

        val result = service.listCaches()

        assertThat(result.map { it.name }).containsExactly(
            "organizationCascadeV2",
            "teamScheduleBranchesV2",
            InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE,
            InMemoryPermissionCacheRegistry.NAME_ADMIN_DATA_SCOPE_CACHE,
        )
    }

    @Test
    @DisplayName("evict - 가상 cache name 은 registry 로 위임")
    fun evict_virtualDelegatesToRegistry() {
        every { registry.handles(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) } returns true
        every { registry.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) } returns (6L to 0L)

        val result = service.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE, "ADMIN001")

        assertThat(result.keysBefore).isEqualTo(6L)
        assertThat(result.keysAfter).isEqualTo(0L)
        verify { registry.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) }
        verify(exactly = 0) { cacheManager.getCache(any()) }
    }

    @Test
    @DisplayName("evict - 일반 cache name 은 CacheManager.clear 호출")
    fun evict_managedClearsCacheManager() {
        every { registry.handles("teamScheduleBranchesV2") } returns false
        val cache = mockk<Cache>(relaxed = true)
        every { cacheManager.getCache("teamScheduleBranchesV2") } returns cache

        service.evict("teamScheduleBranchesV2", "ADMIN001")

        verify { cache.clear() }
    }

    @Test
    @DisplayName("evictAll - 모든 cache name (managed + in-memory) 순회 evict")
    fun evictAll_iteratesAllCaches() {
        every { cacheManager.cacheNames } returns setOf("teamScheduleBranchesV2")
        every { cacheManager.getCache(any()) } returns mockk<Cache>(relaxed = true)
        every { registry.list() } returns listOf(
            InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE to 4L,
        )
        every { registry.handles("teamScheduleBranchesV2") } returns false
        every { registry.handles(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) } returns true
        every { registry.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) } returns (4L to 0L)

        val results = service.evictAll("ADMIN001")

        assertThat(results.map { it.cacheName }).containsExactlyInAnyOrder(
            "teamScheduleBranchesV2",
            InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE,
        )
        verify { registry.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE) }
    }
}
