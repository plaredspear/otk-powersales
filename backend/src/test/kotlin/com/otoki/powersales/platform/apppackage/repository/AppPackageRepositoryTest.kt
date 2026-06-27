package com.otoki.powersales.platform.apppackage.repository

import com.otoki.powersales.platform.apppackage.entity.AppPackage
import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * AppPackageRepository QueryDSL 전환 검증 — `findMaxForceUpdateVersionCode`, `clearLatest`.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class AppPackageRepositoryTest {

    @Autowired
    private lateinit var appPackageRepository: AppPackageRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    @DisplayName("findMaxForceUpdateVersionCode - 해당 플랫폼 force_update=true 중 최대 version_code 만 반환")
    fun maxForceUpdate() {
        persist(AppPlatform.ANDROID, versionCode = 10, forceUpdate = true)
        persist(AppPlatform.ANDROID, versionCode = 30, forceUpdate = true)
        persist(AppPlatform.ANDROID, versionCode = 50, forceUpdate = false) // 강제 아님 → 제외
        persist(AppPlatform.IOS, versionCode = 99, forceUpdate = true) // 다른 플랫폼 → 제외
        em.clear()

        val result = appPackageRepository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID)

        assertThat(result).isEqualTo(30)
    }

    @Test
    @DisplayName("findMaxForceUpdateVersionCode - 강제 버전이 없으면 null")
    fun maxForceUpdate_none() {
        persist(AppPlatform.ANDROID, versionCode = 10, forceUpdate = false)
        em.clear()

        assertThat(appPackageRepository.findMaxForceUpdateVersionCode(AppPlatform.ANDROID)).isNull()
    }

    @Test
    @DisplayName("clearLatest - 해당 플랫폼 is_latest=true 만 해제하고 다른 플랫폼/이미 false 는 건드리지 않는다")
    fun clearLatest() {
        val androidLatest = persist(AppPlatform.ANDROID, versionCode = 20, isLatest = true)
        val androidOld = persist(AppPlatform.ANDROID, versionCode = 10, isLatest = false)
        val iosLatest = persist(AppPlatform.IOS, versionCode = 20, isLatest = true)
        em.clear()

        val updated = appPackageRepository.clearLatest(AppPlatform.ANDROID)
        em.clear()

        assertThat(updated).isEqualTo(1)
        assertThat(appPackageRepository.findById(androidLatest.id).get().isLatest).isFalse()
        assertThat(appPackageRepository.findById(androidOld.id).get().isLatest).isFalse()
        // 다른 플랫폼 최신 지정은 보존
        assertThat(appPackageRepository.findById(iosLatest.id).get().isLatest).isTrue()
    }

    private fun persist(
        platform: AppPlatform,
        versionCode: Long,
        forceUpdate: Boolean = false,
        isLatest: Boolean = false,
    ): AppPackage {
        val pkg = AppPackage(
            platform = platform,
            versionName = "1.0.$versionCode",
            versionCode = versionCode,
            forceUpdate = forceUpdate,
            releaseNote = "note",
            fileUniqueKey = "uploads/app-package/$platform/$versionCode/file",
            fileName = "app",
            fileSize = 1024,
            isLatest = isLatest,
        )
        return em.persistAndFlush(pkg)
    }
}
