package com.otoki.powersales.admin.tools.feature.service

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 기능 토글(feature flag) 상태의 Redis 저장/조회.
 *
 * ## 저장 구조
 * - `feature_toggle:disabled:<code>` → "1" (해당 flag 가 **비활성** 일 때만 존재). 키 부재 = 활성.
 * - `feature_toggle:reason:<code>`   → 비활성 사유 문구 (관리자 입력, 선택).
 *
 * "비활성일 때만 키 존재" 로 두어, 기본값(활성)은 Redis 에 아무것도 없는 상태와 일치시킨다.
 * TTL 없이 영구 저장하므로 앱 재시작 후에도 상태가 유지된다.
 *
 * ## 가용성 원칙
 * Redis 연결 실패 시 조회는 **활성으로 폴백**한다. 개발자 도구의 차단 상태를 못 읽었다고 해서
 * 등록 전 기능이 막히면 Redis 장애 한 번에 서비스가 마비되므로, "못 읽으면 열어둔다"가 안전하다.
 * (반대로 관리자가 명시적으로 끈 flag 는 Redis 정상일 때만 반영된다.)
 */
@Component
class FeatureToggleStore(
    /** Redis 미사용 환경 (test profile 등) 에서는 빈 미등록 — null 허용. */
    private val redisTemplate: RedisTemplate<String, String>?,
) {
    private val log = LoggerFactory.getLogger(FeatureToggleStore::class.java)

    /**
     * flag 의 현재 상태 조회. Redis 미가동/장애 시 활성(사유 없음) 으로 폴백.
     */
    fun getState(flag: FeatureFlag): FeatureToggleState {
        val template = redisTemplate ?: return FeatureToggleState(enabled = true, reason = null)
        return try {
            val disabled = template.opsForValue().get(disabledKey(flag)) != null
            val reason = if (disabled) template.opsForValue().get(reasonKey(flag)) else null
            FeatureToggleState(enabled = !disabled, reason = reason)
        } catch (e: Exception) {
            log.warn("feature_toggle 상태 조회 실패 → 활성 폴백: flag={}", flag.code, e)
            FeatureToggleState(enabled = true, reason = null)
        }
    }

    /**
     * flag 의 활성/비활성 + 사유를 기록한다. Redis 미가동이면 예외.
     *
     * - `enabled = true` → 두 키 모두 제거 (기본 상태로 복귀).
     * - `enabled = false` → disabled 키 세팅 + reason 키 세팅/제거.
     */
    fun setState(flag: FeatureFlag, enabled: Boolean, reason: String?) {
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 기능 토글을 변경할 수 없습니다")
        if (enabled) {
            template.delete(listOf(disabledKey(flag), reasonKey(flag)))
            return
        }
        template.opsForValue().set(disabledKey(flag), "1")
        val trimmed = reason?.trim()
        if (trimmed.isNullOrEmpty()) {
            template.delete(reasonKey(flag))
        } else {
            template.opsForValue().set(reasonKey(flag), trimmed)
        }
    }

    private fun disabledKey(flag: FeatureFlag) = "feature_toggle:disabled:${flag.code}"
    private fun reasonKey(flag: FeatureFlag) = "feature_toggle:reason:${flag.code}"
}

/** flag 의 현재 상태. [reason] 은 비활성일 때 관리자가 입력한 사유(없을 수 있음). */
data class FeatureToggleState(
    val enabled: Boolean,
    val reason: String?,
)
