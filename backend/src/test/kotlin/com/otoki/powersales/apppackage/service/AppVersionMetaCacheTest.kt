package com.otoki.powersales.apppackage.service

import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.repository.AppPackageRepository
import com.otoki.powersales.common.config.CacheConfig
import com.otoki.powersales.common.config.DomainProperties
import com.otoki.powersales.common.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Optional

/**
 * 버전 체크 메타 캐싱(provider) + 무효화(evict) 단위 테스트.
 *
 * `@Cacheable` 은 프록시 부재인 단위 테스트에서 미적용이므로, 여기서는 (1) 메타 조립 로직과 강제 경계
 * 판정 동등성, (2) admin write 시 evict 가 실제로 호출되는지, (3) evict 가 트랜잭션 커밋 후로 지연되는지를
 * 검증한다. Redis 장애 fallback(CacheErrorHandler)은 인프라 동작이라 본 단위 테스트 범위 밖이다.
 */
@DisplayName("버전 체크 메타 캐시")
class AppVersionMetaCacheTest {

    private val repository = mockk<AppPackageRepository>(relaxed = true)
    private val cacheManager = mockk<CacheManager>(relaxed = true)

    private fun entity(
        id: Long = 1L,
        platform: AppPlatform = AppPlatform.ANDROID,
        versionCode: Long = 10,
        versionName: String = "1.0.0",
    ) = AppPackage(
        id = id,
        platform = platform,
        versionName = versionName,
        versionCode = versionCode,
        fileUniqueKey = "key-$id",
        fileName = "app-$id.apk",
        fileSize = 100,
        isLatest = true,
    )

    @Nested
    @DisplayName("loadMeta")
    inner class LoadMeta {

        @Test
        @DisplayName("배포본 없음 → EMPTY 메타")
        fun noPackage() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns null
            every { repository.findTopByPlatformOrderByVersionCodeDesc(AppPlatform.ANDROID) } returns null

            val meta = AppVersionMetaProvider(repository, cacheManager).loadMeta(AppPlatform.ANDROID)

            assertThat(meta.hasLatest).isFalse()
            assertThat(meta.latestVersionCode).isNull()
            assertThat(meta.maxForceUpdateVersionCode).isNull()
        }

        @Test
        @DisplayName("최신 메타 + 강제 경계(maxForceUpdateVersionCode) 조립")
        fun assembleMeta() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns
                entity(id = 9L, versionCode = 30)
            every { repository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID) } returns 25

            val meta = AppVersionMetaProvider(repository, cacheManager).loadMeta(AppPlatform.ANDROID)

            assertThat(meta.hasLatest).isTrue()
            assertThat(meta.latestPackageId).isEqualTo(9L)
            assertThat(meta.latestVersionCode).isEqualTo(30)
            assertThat(meta.maxForceUpdateVersionCode).isEqualTo(25)
        }
    }

    @Nested
    @DisplayName("강제 경계 판정 동등성 (maxForceUpdateVersionCode > 요청 vc)")
    inner class ForceBoundary {

        private val storageService = mockk<StorageService>(relaxed = true)
        private val provider = AppVersionMetaProvider(repository, cacheManager)
        private val mobileService = MobileAppPackageService(
            repository, storageService, ManifestPlistBuilder(), IosInstallPageBuilder(), provider,
        )

        @Test
        @DisplayName("요청 vc < max 강제 vc → forceUpdate=true")
        fun forced() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns entity(versionCode = 30)
            every { repository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID) } returns 20

            val r = mobileService.check(AppPlatform.ANDROID, 5, "https://api.example.com")

            assertThat(r.updateAvailable).isTrue()
            assertThat(r.forceUpdate).isTrue()
        }

        @Test
        @DisplayName("요청 vc >= max 강제 vc → forceUpdate=false (강제 버전을 이미 지남)")
        fun notForcedWhenPastBoundary() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns entity(versionCode = 30)
            every { repository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID) } returns 10

            val r = mobileService.check(AppPlatform.ANDROID, 15, "https://api.example.com")

            assertThat(r.updateAvailable).isTrue() // 최신 30 > 15
            assertThat(r.forceUpdate).isFalse()    // 강제 경계 10 <= 15
        }

        @Test
        @DisplayName("강제 버전 없음(null) → forceUpdate=false")
        fun noForceVersion() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns entity(versionCode = 30)
            every { repository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID) } returns null

            val r = mobileService.check(AppPlatform.ANDROID, 5, "https://api.example.com")

            assertThat(r.forceUpdate).isFalse()
        }
    }

    @Nested
    @DisplayName("무효화(evict) — admin write 직후 호출")
    inner class Evict {

        private val storageService = mockk<StorageService>(relaxed = true)
        private val provider = mockk<AppVersionMetaProvider>(relaxed = true)
        private val domainProperties =
            DomainProperties(api = "dev-powersalesapi.otoki.com", admin = "dev-admin.otoki.com")
        private val adminService = AdminAppPackageService(
            repository, storageService, IpaMetadataExtractor(), mockk(relaxed = true),
            domainProperties, provider,
        )

        init {
            // relaxed save 가 더미 AppPackage 를 반환하면 AppPackageDetailDto.from 캐스팅 실패 →
            // 입력 엔티티를 그대로 반환하게 한다.
            every { repository.save(any()) } answers { firstArg<AppPackage>() }
        }

        @Test
        @DisplayName("setLatest → 해당 platform evict")
        fun setLatestEvicts() {
            every { repository.findById(2L) } returns Optional.of(entity(id = 2L, platform = AppPlatform.IOS))

            adminService.setLatest(2L)

            verify(exactly = 1) { provider.evict(AppPlatform.IOS) }
        }

        @Test
        @DisplayName("setForceUpdate → 해당 platform evict")
        fun setForceUpdateEvicts() {
            every { repository.findById(3L) } returns Optional.of(entity(id = 3L, platform = AppPlatform.ANDROID))

            adminService.setForceUpdate(3L, true)

            verify(exactly = 1) { provider.evict(AppPlatform.ANDROID) }
        }

        @Test
        @DisplayName("updateReleaseNote → 해당 platform evict")
        fun updateReleaseNoteEvicts() {
            every { repository.findById(4L) } returns Optional.of(entity(id = 4L, platform = AppPlatform.ANDROID))

            adminService.updateReleaseNote(4L, "note")

            verify(exactly = 1) { provider.evict(AppPlatform.ANDROID) }
        }

        @Test
        @DisplayName("delete(비최신) → 해당 platform evict")
        fun deleteEvicts() {
            every { repository.findById(5L) } returns
                Optional.of(entity(id = 5L, platform = AppPlatform.IOS).apply { isLatest = false })

            adminService.delete(5L)

            verify(exactly = 1) { provider.evict(AppPlatform.IOS) }
        }
    }

    @Nested
    @DisplayName("evict 트랜잭션 타이밍")
    inner class EvictTiming {

        private val cache = mockk<Cache>(relaxed = true)
        private val provider = AppVersionMetaProvider(repository, cacheManager)

        init {
            every { cacheManager.getCache(CacheConfig.CACHE_APP_VERSION_META) } returns cache
        }

        @Test
        @DisplayName("트랜잭션 없으면 즉시 evict")
        fun evictsImmediatelyWithoutTransaction() {
            provider.evict(AppPlatform.ANDROID)

            verify(exactly = 1) { cache.evictIfPresent(AppPlatform.ANDROID) }
        }

        @Test
        @DisplayName("트랜잭션 활성 시 커밋 전엔 evict 안 함 → afterCommit 에 실행")
        fun defersEvictUntilAfterCommit() {
            TransactionSynchronizationManager.initSynchronization()
            try {
                provider.evict(AppPlatform.IOS)

                // 커밋 전: 아직 evict 되지 않음 (동기화에 등록만 됨)
                verify(exactly = 0) { cache.evictIfPresent(any()) }
                val syncs = TransactionSynchronizationManager.getSynchronizations()
                assertThat(syncs).hasSize(1)

                // 커밋 시뮬레이션 → afterCommit 콜백 실행
                syncs.forEach(TransactionSynchronization::afterCommit)
                verify(exactly = 1) { cache.evictIfPresent(AppPlatform.IOS) }
            } finally {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }
    }
}
