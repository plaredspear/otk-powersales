package com.otoki.powersales.auth.permission

import com.github.benmanes.caffeine.cache.Caffeine
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 관리자 SF 권한 set 의 in-memory cache.
 *
 * ## 도입 배경
 *
 * spec #808 의 expandAllDataBits 일반화 이후 user 권한 set 이 90+ entity × 4 op + system 5 ≈ 365
 * key 로 확장되어 JWT claim 으로 운반 시 토큰 byte 가 8KB 를 초과 → nginx 의
 * `large_client_header_buffers` 기본 한도 (4 8k) 를 초과해 `400 Request Header Or Cookie Too Large`
 * 발생. JWT 에서 permissions claim 을 제거하고 가드 평가 시점에 본 캐시로 lazy lookup.
 *
 * ## 정합
 *
 * - 입력: `userId`
 * - 출력: [SfPermissionResolver.resolveForUser] 산출 결과 그대로 (entity:R/C/E/D + SYSTEM:* 평탄화)
 * - TTL: 5분 — SF Profile / PermissionSetAssignment 변경 후 즉시 반영이 필요하면 [invalidate] 명시 호출
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
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(10_000)
        .build<Long, Set<String>>()

    /**
     * user 의 permission set 을 반환. cache miss 시 DB 조회 후 5분 TTL 로 적재.
     */
    fun get(userId: Long): Set<String> {
        return cache.get(userId) { resolveFromDb(userId) }
    }

    /**
     * 특정 user 의 cache entry 제거 — SF 권한 변경 즉시 반영용.
     */
    fun invalidate(userId: Long) {
        cache.invalidate(userId)
    }

    /**
     * 전체 cache 제거 — 관리자가 권한 모델 일괄 갱신 후 명시 호출.
     */
    fun invalidateAll() {
        cache.invalidateAll()
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
