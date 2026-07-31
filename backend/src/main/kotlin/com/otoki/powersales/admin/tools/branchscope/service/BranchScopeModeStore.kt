package com.otoki.powersales.admin.tools.branchscope.service

import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 지점 스코프 방식([BranchScopeMode]) 의 Redis 저장/조회 —
 * `FeatureToggleStore` 와 동일한 "기본값은 키 부재" 규약.
 *
 * - key `branch_scope:mode` → `LEGACY` 일 때만 존재. 키 부재 = [BranchScopeMode.DEFAULT] (UNIFIED).
 * - TTL 없이 영구 저장 → 앱 재시작 후에도 선택이 유지된다.
 * - Redis 미가동/장애 시 조회는 **기본값(UNIFIED) 폴백** — 개발자 도구 상태를 못 읽었다고 해서
 *   대시보드가 예전 동작으로 몰래 되돌아가면 비교 결과를 신뢰할 수 없다.
 */
@Component
class BranchScopeModeStore(
    /** Redis 미사용 환경 (test profile 등) 에서는 빈 미등록 — null 허용. */
    private val redisTemplate: RedisTemplate<String, String>?,
) {
    private val log = LoggerFactory.getLogger(BranchScopeModeStore::class.java)

    fun getMode(): BranchScopeMode {
        val template = redisTemplate ?: return BranchScopeMode.DEFAULT
        return try {
            BranchScopeMode.fromNameOrNull(template.opsForValue().get(MODE_KEY)) ?: BranchScopeMode.DEFAULT
        } catch (e: Exception) {
            log.warn("branch_scope 모드 조회 실패 → 기본값({}) 폴백", BranchScopeMode.DEFAULT, e)
            BranchScopeMode.DEFAULT
        }
    }

    /** 기본값(UNIFIED)이면 키를 지우고, 그 외 모드는 이름을 그대로 기록한다. Redis 미가동이면 예외. */
    fun setMode(mode: BranchScopeMode) {
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 지점 스코프 방식을 변경할 수 없습니다")
        if (mode == BranchScopeMode.DEFAULT) {
            template.delete(MODE_KEY)
            return
        }
        template.opsForValue().set(MODE_KEY, mode.name)
    }

    companion object {
        private const val MODE_KEY = "branch_scope:mode"
    }
}
