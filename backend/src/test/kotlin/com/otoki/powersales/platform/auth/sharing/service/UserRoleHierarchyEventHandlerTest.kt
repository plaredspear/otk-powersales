package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.event.UserRoleChangedEvent
import com.otoki.powersales.common.config.CacheConfig
import com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyEventHandler
import com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyTraversal
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * UserRoleHierarchyEventHandler 단위 테스트 (spec #788).
 *
 * 검증 대상:
 * - 이벤트 수신 시 recomputeSubtree 호출 (userRoleId 전달)
 * - cache evict 호출 (memberGroupIds)
 * - handler 실패 시 트랜잭션 보존 (예외 swallow + logger.error)
 * - CREATED / UPDATED / REMOVED changeType 모두 동일 흐름
 */
@DisplayName("UserRoleHierarchyEventHandler — UserRole 변경 자동 invalidation (spec #788)")
class UserRoleHierarchyEventHandlerTest {

    private val hierarchyTraversal: UserRoleHierarchyTraversal = mockk(relaxed = true)
    private val cacheManager: CacheManager = mockk(relaxed = true)
    private val memberGroupCache: Cache = mockk(relaxed = true)

    private val handler = UserRoleHierarchyEventHandler(hierarchyTraversal, cacheManager)

    @Nested
    @DisplayName("정상 흐름 — recomputeSubtree + cache evict")
    inner class NormalFlow {

        @Test
        @DisplayName("CREATED 이벤트 → recomputeSubtree(userRoleId) + memberGroupIds cache evict")
        fun createdEvent() {
            every { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) } returns memberGroupCache
            every { memberGroupCache.clear() } just runs

            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 100L, changeType = UserRoleChangedEvent.ChangeType.CREATED),
            )

            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(100L) }
            verify(exactly = 1) { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) }
            verify(exactly = 1) { memberGroupCache.clear() }
        }

        @Test
        @DisplayName("UPDATED 이벤트 → recomputeSubtree(userRoleId) 호출")
        fun updatedEvent() {
            every { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) } returns memberGroupCache

            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 200L, changeType = UserRoleChangedEvent.ChangeType.UPDATED),
            )

            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(200L) }
        }

        @Test
        @DisplayName("REMOVED 이벤트 → recomputeSubtree(userRoleId) 호출")
        fun removedEvent() {
            every { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) } returns memberGroupCache

            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 300L, changeType = UserRoleChangedEvent.ChangeType.REMOVED),
            )

            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(300L) }
        }
    }

    @Nested
    @DisplayName("cache 미존재 — 안전 skip")
    inner class CacheAbsence {

        @Test
        @DisplayName("memberGroupIds cache 미존재 (null) → evict skip + 예외 0건")
        fun cacheAbsentNoException() {
            every { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) } returns null

            // 예외 throw 0건 — 정상 종료
            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 400L, changeType = UserRoleChangedEvent.ChangeType.UPDATED),
            )

            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(400L) }
        }
    }

    @Nested
    @DisplayName("Q5 옵션 1 — handler 실패 시 트랜잭션 보존")
    inner class FailureFallback {

        @Test
        @DisplayName("recomputeSubtree 예외 → handler 가 swallow (예외 propagate 안 함)")
        fun recomputeSubtreeFailureSwallowed() {
            every { hierarchyTraversal.recomputeSubtree(any()) } throws RuntimeException("snapshot DB 일시 장애")

            // handler 가 예외를 swallow — 호출자에게 propagate 안 함 (트랜잭션 보존)
            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 500L, changeType = UserRoleChangedEvent.ChangeType.UPDATED),
            )

            // recomputeSubtree 는 1회 호출 시도됐음
            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(500L) }
        }

        @Test
        @DisplayName("cache evict 예외 → handler 가 swallow")
        fun cacheEvictFailureSwallowed() {
            every { cacheManager.getCache(CacheConfig.CACHE_MEMBER_GROUP_IDS) } returns memberGroupCache
            every { memberGroupCache.clear() } throws RuntimeException("Redis connection lost")

            handler.onUserRoleChanged(
                UserRoleChangedEvent(userRoleId = 600L, changeType = UserRoleChangedEvent.ChangeType.UPDATED),
            )

            verify(exactly = 1) { hierarchyTraversal.recomputeSubtree(600L) }
        }
    }
}
