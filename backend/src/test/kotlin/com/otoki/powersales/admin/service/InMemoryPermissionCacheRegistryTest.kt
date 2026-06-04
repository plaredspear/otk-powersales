package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.auth.permission.AdminPermissionCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InMemoryPermissionCacheRegistry 테스트")
class InMemoryPermissionCacheRegistryTest {

    private val permissionCache = mockk<AdminPermissionCache>(relaxed = true)
    private val dataScopeCache = mockk<AdminDataScopeCache>(relaxed = true)
    private val registry = InMemoryPermissionCacheRegistry(permissionCache, dataScopeCache)

    @Test
    @DisplayName("list - 두 가상 cache name 과 추정 개수 반환")
    fun list_returnsBothVirtualCaches() {
        every { permissionCache.estimatedSize() } returns 7
        every { dataScopeCache.estimatedSize() } returns 3

        val result = registry.list()

        assertThat(result).containsExactly(
            InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE to 7L,
            InMemoryPermissionCacheRegistry.NAME_ADMIN_DATA_SCOPE_CACHE to 3L,
        )
    }

    @Test
    @DisplayName("handles - 등록된 가상 cache name 만 true")
    fun handles_onlyVirtualNames() {
        assertThat(registry.handles(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE)).isTrue()
        assertThat(registry.handles(InMemoryPermissionCacheRegistry.NAME_ADMIN_DATA_SCOPE_CACHE)).isTrue()
        assertThat(registry.handles("teamScheduleBranchesV2")).isFalse()
        assertThat(registry.handles("adminPermissionCache")).isFalse() // mem: prefix 없음
    }

    @Test
    @DisplayName("evict - 해당 캐시 invalidateAll 호출 + before/after 추정 개수 반환")
    fun evict_invalidatesAndReturnsSizes() {
        every { permissionCache.estimatedSize() } returnsMany listOf(5L, 0L)

        val (before, after) = registry.evict(InMemoryPermissionCacheRegistry.NAME_ADMIN_PERMISSION_CACHE)

        assertThat(before).isEqualTo(5L)
        assertThat(after).isEqualTo(0L)
        verify { permissionCache.invalidateAll() }
    }

    @Test
    @DisplayName("evict - 미등록 가상 cache name 은 예외")
    fun evict_unknownNameThrows() {
        assertThatThrownBy { registry.evict("mem:unknown") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("invalidateAll - 두 캐시 모두 invalidateAll 호출")
    fun invalidateAll_clearsBoth() {
        registry.invalidateAll()

        verify { permissionCache.invalidateAll() }
        verify { dataScopeCache.invalidateAll() }
    }
}
