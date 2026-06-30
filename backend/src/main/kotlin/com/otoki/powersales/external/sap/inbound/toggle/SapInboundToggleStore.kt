package com.otoki.powersales.external.sap.inbound.toggle

import com.otoki.powersales.admin.sap.SapInboundCatalog
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * SAP 인바운드 endpoint 별 처리 활성/비활성 상태 저장소 (Redis backend).
 *
 * - **활성** (기본): 기존과 동일하게 컨트롤러/서비스가 적재 처리.
 * - **비활성**: [SapInboundToggleInterceptor] 가 적재 처리를 건너뛰고 정상(200/OK) 응답만 반환.
 *
 * 저장 규약:
 * - 키: `sap:inbound:enabled:<endpointPath>` (예: `sap:inbound:enabled:/api/v1/sap/account`)
 * - **비활성일 때만** 값 `"false"` 를 기록한다. 키 부재 = 활성(기본값).
 *   → Redis 가 비어 있거나(초기 상태) 장애로 조회에 실패해도 자연히 "활성" 으로 동작한다.
 *
 * Redis 장애 정책: 모든 Redis 예외를 삼키고 **활성(true)** 으로 fallback 한다 (요구사항).
 * 단, 토글 변경(set)은 Redis 에 기록하지 못하면 예외를 전파한다 (운영자에게 실패를 알리기 위함).
 */
@Component
class SapInboundToggleStore(
    /** Redis 미사용 환경(test profile 등)에서는 빈 미등록 — null 허용. null 이면 항상 활성. */
    private val redisTemplate: StringRedisTemplate?,
) {

    private val log = LoggerFactory.getLogger(SapInboundToggleStore::class.java)

    /**
     * endpointPath 의 처리 활성 여부. 키 부재 / Redis 장애 / 미등록 환경 모두 true(활성).
     */
    fun isEnabled(endpointPath: String): Boolean {
        val template = redisTemplate ?: return true
        return try {
            // "false" 가 명시적으로 기록된 경우에만 비활성. 그 외(null 포함)는 활성.
            template.opsForValue().get(key(endpointPath)) != VALUE_DISABLED
        } catch (ex: Exception) {
            log.warn(
                "[SAP_INBOUND_TOGGLE] Redis 조회 실패 — 기본값(활성)으로 처리. endpoint={} cause={}",
                endpointPath, ex.message
            )
            true
        }
    }

    /**
     * 전체 카탈로그 endpoint 의 활성 여부 맵. 카탈로그에 없는 endpoint 는 다루지 않는다.
     * Redis 장애 시 전부 활성으로 fallback.
     */
    fun getAllStates(): Map<String, Boolean> {
        return SapInboundCatalog.ITEMS.associate { it.endpointPath to isEnabled(it.endpointPath) }
    }

    /**
     * endpointPath 의 처리 활성 여부 설정.
     * - enabled=true  → 키 삭제 (기본값 = 활성 으로 환원)
     * - enabled=false → 값 "false" 기록
     *
     * Redis 미등록/장애 시 [IllegalStateException] 전파 (운영자에게 실패 통지).
     */
    fun setEnabled(endpointPath: String, enabled: Boolean) {
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 SAP 인바운드 토글을 변경할 수 없습니다.")
        try {
            if (enabled) {
                template.delete(key(endpointPath))
            } else {
                template.opsForValue().set(key(endpointPath), VALUE_DISABLED)
            }
            log.info("[SAP_INBOUND_TOGGLE] endpoint={} enabled={}", endpointPath, enabled)
        } catch (ex: Exception) {
            log.error("[SAP_INBOUND_TOGGLE] Redis 기록 실패. endpoint={} enabled={}", endpointPath, enabled, ex)
            throw IllegalStateException("SAP 인바운드 토글 상태를 저장하지 못했습니다 (Redis 오류).", ex)
        }
    }

    private fun key(endpointPath: String): String = "$KEY_PREFIX$endpointPath"

    companion object {
        const val KEY_PREFIX: String = "sap:inbound:enabled:"
        const val VALUE_DISABLED: String = "false"
    }
}
