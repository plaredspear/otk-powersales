package com.otoki.powersales.auth.permission

import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.util.Optional

@DisplayName("AdminPermissionCache 테스트 (Redis 전환)")
class AdminPermissionCacheTest {

    private val resolver = mockk<SfPermissionResolver>()
    private val userRepository = mockk<UserRepository>()
    private val cacheManager = ConcurrentMapCacheManager("admin-permission:v1")
    private val cache = AdminPermissionCache(resolver, userRepository, cacheManager)

    private val user = mockk<User>()

    @Test
    @DisplayName("get - cache miss 시 DB resolve 후 적재, 두 번째 호출은 cache hit (loader 1회만)")
    fun get_missThenHit() {
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { resolver.resolveForUser(user) } returns setOf("team_member_schedule:R")

        val first = cache.get(1L)
        val second = cache.get(1L)

        assertThat(first).containsExactly("team_member_schedule:R")
        assertThat(second).containsExactly("team_member_schedule:R")
        // cache hit 으로 loader (DB resolve) 는 1회만 호출.
        verify(exactly = 1) { resolver.resolveForUser(user) }
    }

    @Test
    @DisplayName("get - user 부재 시 빈 set (모든 권한 차단)")
    fun get_userNotFound() {
        every { userRepository.findById(99L) } returns Optional.empty()

        assertThat(cache.get(99L)).isEmpty()
    }

    @Test
    @DisplayName("invalidate - evict 후 재조회 시 loader 재호출")
    fun invalidate_reloadsAfterEvict() {
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { resolver.resolveForUser(user) } returns setOf("account:R")

        cache.get(1L)
        cache.invalidate(1L)
        cache.get(1L)

        verify(exactly = 2) { resolver.resolveForUser(user) }
    }

    @Test
    @DisplayName("invalidateAll - 전체 clear 후 재조회 시 loader 재호출")
    fun invalidateAll_reloadsAll() {
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { resolver.resolveForUser(user) } returns setOf("account:R")

        cache.get(1L)
        cache.invalidateAll()
        cache.get(1L)

        verify(exactly = 2) { resolver.resolveForUser(user) }
    }

    @Test
    @DisplayName("get - cache 빈 미등록 시 캐시 없이 DB resolve 직접 (NoOp fallback)")
    fun get_noCacheFallback() {
        val emptyManager = ConcurrentMapCacheManager().apply { setCacheNames(emptyList()) }
        val noCacheInstance = AdminPermissionCache(resolver, userRepository, emptyManager)
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { resolver.resolveForUser(user) } returns setOf("account:R")

        // cache 미등록이라 매 호출 resolve.
        noCacheInstance.get(1L)
        noCacheInstance.get(1L)

        verify(exactly = 2) { resolver.resolveForUser(user) }
    }
}
