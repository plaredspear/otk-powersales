package com.otoki.powersales.platform.batch.toggle

import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 스케줄 잡(`@Scheduled` 배치) 별 런타임 활성/비활성 토글 저장소 (Redis backend).
 *
 * - **활성** (기본): 잡 본문이 평소대로 실행된다.
 * - **비활성**: [com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner] 가 본문을 실행하지 않고
 *   `SKIPPED` 이력 1행만 남긴다.
 *
 * 이는 배치 빈 등록 여부로 결정되는 **정적 활성 여부**(`@Profile`/`@ConditionalOnProperty`)와는 별개인,
 * 운영 중 끄고 켜는 **런타임 토글**이다. 정적으로 비활성인(빈 미등록) 잡은 애초에 실행되지 않으므로
 * 본 토글의 영향을 받지 않는다.
 *
 * 저장 규약 (SAP 인바운드 토글과 동일 패턴):
 * - 키: `scheduled-job:enabled:<jobName>` (예: `scheduled-job:enabled:sap-outbox-worker`)
 * - **비활성일 때만** 값 `"false"` 를 기록한다. 키 부재 = 활성(기본값).
 *   → Redis 가 비어 있거나(초기 상태) 장애로 조회에 실패해도 자연히 "활성" 으로 동작한다.
 *
 * 프로파일 정책: SAP 인바운드 토글과 동일하게 **dev/prod 에서만** 동작한다.
 * local/test 등 그 외 프로파일에서는 토글을 적용하지 않아 항상 활성으로 동작하고, 변경(set)도 거부한다.
 *
 * Redis 장애 정책: 모든 Redis 예외를 삼키고 **활성(true)** 으로 fallback 한다.
 * 단, 토글 변경(set)은 Redis 에 기록하지 못하면 예외를 전파한다 (운영자에게 실패를 알리기 위함).
 */
@Component
class ScheduledJobToggleStore(
    /** Redis 미사용 환경(test profile 등)에서는 빈 미등록 — null 허용. null 이면 항상 활성. */
    private val redisTemplate: StringRedisTemplate?,
    environment: Environment,
) {

    private val log = LoggerFactory.getLogger(ScheduledJobToggleStore::class.java)

    /** dev/prod 프로파일에서만 토글이 동작한다. 그 외(local/test)는 항상 활성 + 변경 거부. */
    private val toggleActive: Boolean =
        environment.activeProfiles.any { it in TOGGLE_PROFILES }

    /**
     * jobName 의 런타임 활성 여부. 키 부재 / Redis 장애 / 미등록 환경 / 비대상 프로파일 모두 true(활성).
     */
    fun isEnabled(jobName: String): Boolean {
        if (!toggleActive) return true
        val template = redisTemplate ?: return true
        return try {
            template.opsForValue().get(key(jobName)) != VALUE_DISABLED
        } catch (ex: Exception) {
            log.warn(
                "[SCHEDULED_JOB_TOGGLE] Redis 조회 실패 — 기본값(활성)으로 처리. jobName={} cause={}",
                jobName, ex.message
            )
            true
        }
    }

    /**
     * 카탈로그 전 잡의 런타임 활성 여부 맵. Redis 장애 시 전부 활성으로 fallback.
     */
    fun getAllStates(): Map<String, Boolean> {
        return ScheduledJobCatalog.JOB_NAMES.associateWith { isEnabled(it) }
    }

    /**
     * jobName 의 런타임 활성 여부 설정.
     * - enabled=true  → 키 삭제 (기본값 = 활성 으로 환원)
     * - enabled=false → 값 "false" 기록
     *
     * Redis 미등록/장애 시 [IllegalStateException] 전파 (운영자에게 실패 통지).
     */
    fun setEnabled(jobName: String, enabled: Boolean) {
        if (!toggleActive) {
            throw IllegalStateException("스케줄 잡 토글은 dev/prod 환경에서만 변경할 수 있습니다.")
        }
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 스케줄 잡 토글을 변경할 수 없습니다.")
        try {
            if (enabled) {
                template.delete(key(jobName))
            } else {
                template.opsForValue().set(key(jobName), VALUE_DISABLED)
            }
            log.info("[SCHEDULED_JOB_TOGGLE] jobName={} enabled={}", jobName, enabled)
        } catch (ex: Exception) {
            log.error("[SCHEDULED_JOB_TOGGLE] Redis 기록 실패. jobName={} enabled={}", jobName, enabled, ex)
            throw IllegalStateException("스케줄 잡 토글 상태를 저장하지 못했습니다 (Redis 오류).", ex)
        }
    }

    private fun key(jobName: String): String = "$KEY_PREFIX$jobName"

    companion object {
        const val KEY_PREFIX: String = "scheduled-job:enabled:"
        const val VALUE_DISABLED: String = "false"

        /** 토글이 동작하는 프로파일 (SAP 인바운드 토글과 동일 정책). */
        private val TOGGLE_PROFILES = setOf("dev", "prod")
    }
}
