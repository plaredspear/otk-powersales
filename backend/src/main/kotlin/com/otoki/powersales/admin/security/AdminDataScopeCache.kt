package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.config.CacheConfig
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.Callable

/**
 * 관리자 [DataScope] 의 Redis cache.
 *
 * ## 도입 배경
 *
 * `WebAdminContextFilter` 가 매 admin API 요청마다 [AdminDataScopeService.resolve] 를 호출하면서
 * SF 권한 매트릭스 조회 (user / user_role_hierarchy / group ×2 / profile_flags / permission_set_assignment /
 * permission_set_flags / sharing_rule / sharing_rule_condition / profile_record_type /
 * permission_set_record_type ≈ 10+ 쿼리) 가 화면 진입 시 동시 3개 API 가 호출되면 30+ DB roundtrip 로
 * 증폭되는 사례를 해소.
 *
 * ## Caffeine → Redis 전환
 *
 * 이전 구현은 `Caffeine.newBuilder()` 자체 in-memory 인스턴스였다. [com.otoki.powersales.auth.permission.AdminPermissionCache]
 * 와 동일하게 멀티 인스턴스 환경에서 invalidate 가 단일 JVM 만 비워 DataScope stale → 조회 범위 오판
 * (권한 어긋남) 을 유발했다. Spring [CacheManager] (dev/prod = RedisCacheManager) 공유 캐시로 전환.
 * local/test 는 `@Primary NoOpCacheManager` 라 항상 miss → 매번 resolve (무캐시, stale 없음).
 *
 * ## 정합
 *
 * - 입력: `userId` (= [WebUserPrincipal.userId]) — principal 의 나머지 필드 (profileName / costCenterCode 등)
 *   는 모두 principal snapshot 이라 userId 단일 키로 충분.
 * - 출력: [AdminDataScopeService.resolve] (principal 시그니처) 산출 결과 그대로
 * - TTL: 5분 ([CacheConfig.CACHE_ADMIN_DATA_SCOPE]) — 권한 변경 즉시 반영 필요 시 [invalidate] 호출 의무
 *
 * ## 캐시 무효화 책임
 *
 * DataScope 의 입력 (profile / permissionSet / userRole / group 멤버십 / sharingRule) 이 바뀌면
 * [com.otoki.powersales.auth.permission.AdminPermissionCache.invalidate] 와 함께 본 캐시도
 * `invalidate(userId)` 호출 필요. 현재 `AdminPermissionAssignmentService` 와 `AppointmentUserProfileUpdater`
 * 에서 동시 호출.
 */
@Service
class AdminDataScopeCache(
    private val adminDataScopeService: AdminDataScopeService,
    private val cacheManager: CacheManager,
) {

    /**
     * principal 의 DataScope 를 반환. cache miss 시 [AdminDataScopeService.resolve] 호출 후 5분 TTL 적재.
     *
     * cache 빈 미등록 (이론상) 시 캐시 없이 resolve 직접 호출.
     */
    fun get(principal: WebUserPrincipal): DataScope {
        val cache = cacheManager.getCache(CacheConfig.CACHE_ADMIN_DATA_SCOPE)
            ?: return adminDataScopeService.resolve(principal)
        return cache.get(principal.userId, Callable { adminDataScopeService.resolve(principal) }) as DataScope
    }

    /**
     * 특정 user 의 cache entry 제거 — SF 권한/조직 변경 즉시 반영용 (전 인스턴스).
     */
    fun invalidate(userId: Long) {
        cacheManager.getCache(CacheConfig.CACHE_ADMIN_DATA_SCOPE)?.evict(userId)
    }

    /**
     * 전체 cache 제거 — 권한 모델 일괄 갱신 후 명시 호출 (전 인스턴스).
     */
    fun invalidateAll() {
        cacheManager.getCache(CacheConfig.CACHE_ADMIN_DATA_SCOPE)?.clear()
    }
}
