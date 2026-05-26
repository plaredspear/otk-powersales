package com.otoki.powersales.admin.security

import com.github.benmanes.caffeine.cache.Caffeine
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.auth.web.WebUserPrincipal
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 관리자 [DataScope] 의 in-memory cache.
 *
 * ## 도입 배경
 *
 * `WebAdminContextFilter` 가 매 admin API 요청마다 [AdminDataScopeService.resolve] 를 호출하면서
 * SF 권한 매트릭스 조회 (user / user_role_hierarchy / group ×2 / profile_flags / permission_set_assignment /
 * permission_set_flags / sharing_rule / sharing_rule_condition / profile_record_type /
 * permission_set_record_type ≈ 10+ 쿼리) 가 화면 진입 시 동시 3개 API 가 호출되면 30+ DB roundtrip 로
 * 증폭되는 사례를 해소.
 *
 * ## 정합
 *
 * - 입력: `userId` (= [WebUserPrincipal.userId])
 * - 출력: [AdminDataScopeService.resolve] (principal 시그니처) 산출 결과 그대로
 * - TTL: 5분 — [AdminPermissionCache] 와 동일. 권한 변경 즉시 반영 필요 시 [invalidate] 호출 의무
 *
 * ## 캐시 무효화 책임
 *
 * DataScope 의 입력 (profile / permissionSet / userRole / group 멤버십 / sharingRule) 이 바뀌면
 * [AdminPermissionCache.invalidate] 와 함께 본 캐시도 `invalidate(userId)` 호출 필요. 현재
 * `AdminPermissionAssignmentService` 와 `AppointmentUserProfileUpdater` 에서 동시 호출.
 */
@Service
class AdminDataScopeCache(
    private val adminDataScopeService: AdminDataScopeService,
) {

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(10_000)
        .build<Long, DataScope>()

    /**
     * principal 의 DataScope 를 반환. cache miss 시 [AdminDataScopeService.resolve] 호출 후 5분 TTL 적재.
     */
    fun get(principal: WebUserPrincipal): DataScope {
        return cache.get(principal.userId) { adminDataScopeService.resolve(principal) }
    }

    /**
     * 특정 user 의 cache entry 제거 — SF 권한/조직 변경 즉시 반영용.
     */
    fun invalidate(userId: Long) {
        cache.invalidate(userId)
    }

    /**
     * 전체 cache 제거 — 권한 모델 일괄 갱신 후 명시 호출.
     */
    fun invalidateAll() {
        cache.invalidateAll()
    }
}
