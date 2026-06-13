package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.common.config.CacheConfig
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.Callable

/**
 * 관리자 SF 권한 set 의 Redis cache.
 *
 * ## 도입 배경
 *
 * spec #808 의 expandAllDataBits 일반화 이후 user 권한 set 이 90+ entity × 4 op + system 5 ≈ 365
 * key 로 확장되어 JWT claim 으로 운반 시 토큰 byte 가 8KB 를 초과 → nginx 의
 * `large_client_header_buffers` 기본 한도 (4 8k) 를 초과해 `400 Request Header Or Cookie Too Large`
 * 발생. JWT 에서 permissions claim 을 제거하고 가드 평가 시점에 본 캐시로 lazy lookup.
 *
 * ## Caffeine → Redis 전환
 *
 * 이전 구현은 `Caffeine.newBuilder()` 자체 in-memory 인스턴스였다. 멀티 인스턴스 (dev/prod) 환경에서
 * [invalidate] 가 호출된 단일 JVM 메모리만 비워, 다른 인스턴스는 최대 5분 TTL 동안 stale snapshot 으로
 * 권한을 잘못 판정하는 치명적 결함이 있었다 (SF 마이그레이션 / 권한 편집 직후 인스턴스 간 권한 불일치).
 * Spring [CacheManager] (dev/prod = RedisCacheManager) 의 공유 캐시로 전환하여 invalidate 가 전 인스턴스에
 * 즉시 반영되도록 한다. local/test 는 `@Primary NoOpCacheManager` 라 항상 miss → 매번 DB resolve (무캐시,
 * stale 없음 — 기존 동작과 동등).
 *
 * ## 정합
 *
 * - 입력: `userId`
 * - 출력: [SfPermissionResolver.resolveForUser] 산출 결과 그대로 (entity:R/C/E/D + SYSTEM:* 평탄화)
 * - TTL: 5분 ([CacheConfig.CACHE_ADMIN_PERMISSION]) — SF Profile / PermissionSetAssignment 변경 후 즉시
 *   반영이 필요하면 [invalidate] 명시 호출. Redis 공유라 evict 가 전 인스턴스 반영.
 *
 * ## 캐시 미스 시 동작
 *
 * UserRepository.findById 로 user 조회 → SfPermissionResolver.resolveForUser 호출.
 * user 가 존재하지 않으면 빈 set 반환 (가드 평가 시 모든 권한 차단 = 401 응답과 동등 효과).
 */
@Service
class AdminPermissionCache(
    private val sfPermissionResolver: SfPermissionResolver,
    private val userRepository: UserRepository,
    private val cacheManager: CacheManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * user 의 permission set 을 반환. cache miss 시 DB 조회 후 5분 TTL 로 적재.
     *
     * cache 빈 미등록 (이론상) 시 캐시 없이 DB resolve 직접 호출.
     */
    fun get(userId: Long): Set<String> {
        val cache = cacheManager.getCache(CacheConfig.CACHE_ADMIN_PERMISSION)
            ?: return resolveFromDb(userId)
        @Suppress("UNCHECKED_CAST")
        return cache.get(userId, Callable { resolveFromDb(userId) }) as Set<String>
    }

    /**
     * 특정 user 의 cache entry 제거 — SF 권한 변경 즉시 반영용 (전 인스턴스).
     */
    fun invalidate(userId: Long) {
        cacheManager.getCache(CacheConfig.CACHE_ADMIN_PERMISSION)?.evict(userId)
    }

    /**
     * 전체 cache 제거 — 관리자가 권한 모델 일괄 갱신 후 명시 호출 (전 인스턴스).
     */
    fun invalidateAll() {
        cacheManager.getCache(CacheConfig.CACHE_ADMIN_PERMISSION)?.clear()
    }

    private fun resolveFromDb(userId: Long): Set<String> {
        val user = userRepository.findById(userId).orElse(null)
        if (user == null) {
            log.warn("[admin-permission-cache] user not found userId={} — empty permission set", userId)
            return emptySet()
        }
        return sfPermissionResolver.resolveForUser(user)
    }
}
