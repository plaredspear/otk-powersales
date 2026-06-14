package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.config.CacheConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

@DisplayName("AdminDataScopeCache 테스트 (Redis 전환)")
class AdminDataScopeCacheTest {

    private val resolveService = mockk<AdminDataScopeService>()
    private val cacheManager = ConcurrentMapCacheManager(CacheConfig.CACHE_ADMIN_DATA_SCOPE)
    private val cache = AdminDataScopeCache(resolveService, cacheManager)

    private val principal = mockk<WebUserPrincipal>()
    private val scope = DataScope(branchCodes = listOf("3234"), isAllBranches = false)

    init {
        every { principal.userId } returns 1L
    }

    @Test
    @DisplayName("get - cache miss 시 resolve 후 적재, 두 번째는 hit (resolve 1회만)")
    fun get_missThenHit() {
        every { resolveService.resolve(principal) } returns scope

        val first = cache.get(principal)
        val second = cache.get(principal)

        assertThat(first).isEqualTo(scope)
        assertThat(second).isEqualTo(scope)
        verify(exactly = 1) { resolveService.resolve(principal) }
    }

    @Test
    @DisplayName("invalidate - evict 후 재조회 시 resolve 재호출")
    fun invalidate_reloadsAfterEvict() {
        every { resolveService.resolve(principal) } returns scope

        cache.get(principal)
        cache.invalidate(1L)
        cache.get(principal)

        verify(exactly = 2) { resolveService.resolve(principal) }
    }

    @Test
    @DisplayName("invalidateAll - clear 후 재조회 시 resolve 재호출")
    fun invalidateAll_reloadsAll() {
        every { resolveService.resolve(principal) } returns scope

        cache.get(principal)
        cache.invalidateAll()
        cache.get(principal)

        verify(exactly = 2) { resolveService.resolve(principal) }
    }

    @Test
    @DisplayName("get - cache 빈 미등록 시 캐시 없이 resolve 직접 (NoOp fallback)")
    fun get_noCacheFallback() {
        val emptyManager = ConcurrentMapCacheManager().apply { setCacheNames(emptyList()) }
        val noCacheInstance = AdminDataScopeCache(resolveService, emptyManager)
        every { resolveService.resolve(principal) } returns scope

        noCacheInstance.get(principal)
        noCacheInstance.get(principal)

        verify(exactly = 2) { resolveService.resolve(principal) }
    }
}
