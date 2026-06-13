package com.otoki.powersales.platform.apppackage.service

import com.otoki.powersales.platform.apppackage.dto.AppVersionMeta
import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.repository.AppPackageRepository
import com.otoki.powersales.common.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 버전 체크 메타([AppVersionMeta])의 Redis 캐시 제공자.
 *
 * 로그인 전 스플래시에서 매 실행 호출되는 무인증 버전 체크의 DB 조회(최신 버전 + force_update 경계)를
 * 캐시한다. presigned downloadUrl 처럼 매번 신선해야 하는 값은 메타에 포함하지 않으므로, downloadUrl
 * 합성은 [MobileAppPackageService] 가 캐시 밖에서 수행한다.
 *
 * ## self-invocation 회피
 *
 * `@Cacheable` 은 Spring AOP 프록시 경유 호출에서만 동작한다. 같은 클래스 내부 호출은 프록시를 거치지
 * 않아 캐시가 무시되므로, 메타 로딩을 [MobileAppPackageService] 와 분리된 별도 빈으로 둔다.
 *
 * ## Redis 장애 fallback
 *
 * [CacheConfig] 의 `CacheErrorHandler` 가 캐시 작업 실패(Redis 연결 불가)를 삼키므로, Redis 가 죽어도
 * 캐시 미스로 간주되어 [loadMeta] 본문(DB 조회)이 그대로 실행된다 — 기능 장애로 번지지 않는다.
 */
@Service
@Transactional(readOnly = true)
class AppVersionMetaProvider(
    private val appPackageRepository: AppPackageRepository,
    private val cacheManager: CacheManager,
) {

    /**
     * 플랫폼별 버전 메타. Redis 캐시(5분 TTL). 미스/Redis 장애 시 DB 에서 조립.
     */
    @Cacheable(value = [CacheConfig.CACHE_APP_VERSION_META], key = "#platform")
    fun loadMeta(platform: AppPlatform): AppVersionMeta {
        val latest = resolveLatest(platform) ?: return AppVersionMeta.EMPTY
        return AppVersionMeta(
            hasLatest = true,
            latestPackageId = latest.id,
            latestVersionName = latest.versionName,
            latestVersionCode = latest.versionCode,
            latestFileUniqueKey = latest.fileUniqueKey,
            releaseNote = latest.releaseNote,
            maxForceUpdateVersionCode = appPackageRepository.findMaxForceUpdateVersionCode(platform),
        )
    }

    /**
     * 플랫폼별 버전 메타 캐시 무효화. 앱 패키지 변경(upload/setLatest/setForceUpdate/
     * updateReleaseNote/delete) 시 호출한다.
     *
     * 진행 중인 트랜잭션이 있으면 **커밋 이후(AFTER_COMMIT)** 로 evict 를 지연시킨다. 트랜잭션 본문
     * 내부(커밋 전)에 evict 하면 (1) 커밋이 롤백될 때 캐시만 비워지는 부정합, (2) evict 직후 ~ 커밋
     * 전 사이에 다른 요청의 loadMeta 가 아직 옛 DB 상태를 재캐싱하는 race 창이 생긴다. 커밋 후 evict
     * 는 이 둘을 모두 제거한다. 트랜잭션이 없으면(테스트/직접 호출) 즉시 evict.
     *
     * `@CacheEvict` 어노테이션 대신 [CacheManager] 직접 사용 — 어노테이션은 메서드 반환 직후(커밋 전)
     * 동작이라 위 타이밍 제어가 불가하고, 같은 빈 내부 호출 시 프록시 미경유로 무시되는 함정도 있다.
     */
    fun evict(platform: AppPlatform) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = evictNow(platform)
                }
            )
        } else {
            evictNow(platform)
        }
    }

    /**
     * Redis DEL 즉시 실행. 실패(연결 불가)는 [CacheConfig] 의 errorHandler 가 삼키지 못하는 직접
     * 호출 경로이므로 여기서 swallow — 캐시 무효화 실패가 admin write 를 깨지 않게 한다(TTL fallback).
     */
    private fun evictNow(platform: AppPlatform) {
        try {
            cacheManager.getCache(CacheConfig.CACHE_APP_VERSION_META)?.evictIfPresent(platform)
        } catch (e: RuntimeException) {
            log.warn("버전 메타 캐시 evict 실패 — 무시(TTL 만료로 자연 정합). platform={}", platform, e)
        }
    }

    private fun resolveLatest(platform: AppPlatform) =
        appPackageRepository.findByPlatformAndIsLatestTrue(platform)
            ?: appPackageRepository.findTopByPlatformOrderByVersionCodeDesc(platform)

    companion object {
        private val log = LoggerFactory.getLogger(AppVersionMetaProvider::class.java)
    }
}
