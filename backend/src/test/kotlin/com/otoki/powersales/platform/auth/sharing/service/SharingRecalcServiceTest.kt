package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.entity.SharingRecalcLog
import com.otoki.powersales.platform.auth.sharing.repository.SharingRecalcLogRepository
import com.otoki.powersales.platform.common.config.CacheConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.OffsetDateTime

/**
 * SharingRecalcService 단위 테스트 (spec #792).
 *
 * Q1~Q4 옵션 1 정합 — Spring CacheManager 위임 + audit log 박제.
 */
@DisplayName("SharingRecalcService — spec #792")
class SharingRecalcServiceTest {

    private val cacheManager = mockk<CacheManager>()
    private val logRepo = mockk<SharingRecalcLogRepository>()
    private val service = SharingRecalcService(cacheManager, logRepo)

    @Test
    @DisplayName("recalcAll — 모든 sharing 관련 cache 일괄 clear + audit log INSERT")
    fun recalcAll() {
        val mockCache = mockk<Cache>(relaxed = true)
        CacheConfig.SHARING_RELATED_CACHE_NAMES.forEach { name ->
            every { cacheManager.getCache(name) } returns mockCache
        }
        val savedSlot = slot<SharingRecalcLog>()
        every { logRepo.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.recalcAll(100L)

        assertThat(result.evictedCount).isEqualTo(CacheConfig.SHARING_RELATED_CACHE_NAMES.size)
        assertThat(savedSlot.captured.scope).isEqualTo("ALL")
        assertThat(savedSlot.captured.triggeredByUserId).isEqualTo(100L)
        verify(exactly = CacheConfig.SHARING_RELATED_CACHE_NAMES.size) { mockCache.clear() }
    }

    @Test
    @DisplayName("recalcSObject — sObject 지정 audit + cache clear")
    fun recalcSObject() {
        val mockCache = mockk<Cache>(relaxed = true)
        CacheConfig.SHARING_RELATED_CACHE_NAMES.forEach { name ->
            every { cacheManager.getCache(name) } returns mockCache
        }
        val savedSlot = slot<SharingRecalcLog>()
        every { logRepo.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.recalcSObject("Account", 100L)

        assertThat(result.evictedCount).isGreaterThan(0)
        assertThat(savedSlot.captured.scope).isEqualTo("SOBJECT")
        assertThat(savedSlot.captured.sObjectName).isEqualTo("Account")
    }

    @Test
    @DisplayName("recalc/all + cache manager 의 일부 cache 미존재 — null 반환 시 skip")
    fun cacheNotFound() {
        every { cacheManager.getCache(any()) } returns null
        every { logRepo.save(any()) } answers { firstArg() }

        val result = service.recalcAll(100L)
        // 모든 cache 가 null 이라 evictedCount = 0
        assertThat(result.evictedCount).isEqualTo(0)
    }

    @Test
    @DisplayName("getStatus — 마지막 log row 반환")
    fun getStatus() {
        every { logRepo.findTopByOrderByTriggeredAtDesc() } returns SharingRecalcLog(
            triggeredAt = OffsetDateTime.now(),
            triggeredByUserId = 100L,
            scope = "ALL",
            evictedCacheCount = 8,
            durationMs = 50,
        )

        val status = service.getStatus()
        assertThat(status.lastRecalcScope).isEqualTo("ALL")
        assertThat(status.lastEvictedCount).isEqualTo(8)
    }

    @Test
    @DisplayName("getStatus — log 없으면 null 필드")
    fun getStatusNoLog() {
        every { logRepo.findTopByOrderByTriggeredAtDesc() } returns null
        val status = service.getStatus()
        assertThat(status.lastRecalcAt).isNull()
        assertThat(status.lastEvictedCount).isNull()
    }
}
