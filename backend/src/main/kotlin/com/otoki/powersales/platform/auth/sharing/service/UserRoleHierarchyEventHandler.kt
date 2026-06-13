package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.event.UserRoleChangedEvent
import com.otoki.powersales.common.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * UserRole 변경 시 hierarchy snapshot + cache 자동 갱신 핸들러.
 *
 * 수신: UserRole 변경 Service 가 명시 발행한 [UserRoleChangedEvent] —
 *   `@TransactionalEventListener(AFTER_COMMIT)` 로 트랜잭션 commit 후 동작.
 *
 * ## 처리 흐름
 *
 * 1. [UserRoleHierarchyTraversal.recomputeSubtree] 호출 — 변경된 UserRole 의 subtree snapshot 재계산
 * 2. `hierarchySubordinates` / `hierarchyAncestorPath` / `memberGroupIds` cache 추가 evict (보완)
 *
 * `recomputeSubtree` 자체가 `@CacheEvict(allEntries = true)` 로 hierarchy cache 전체 evict 를
 * 이미 수행하므로 추가 evict 는 보완 차원 (memberGroupIds 등 다른 cache 도 정합 보장).
 *
 * ## Q5 옵션 1 — handler 실패 시 트랜잭션 보존
 *
 * handler 가 예외를 throw 하면 admin 운영 차단 가능 — 본 핸들러는 모든 예외를 logger.error 로
 * 출력하고 swallow. 운영자는 `recomputeAll()` 수동 batch 로 복구.
 *
 * ## Q4 옵션 1 — 동시성 단순 처리
 *
 * UserRole 동시 변경 빈도 극히 낮음 (250개 정책). distributed lock 미도입 — Spring event 기본 순차 처리.
 * 동시 처리 충돌 시 마지막 변경 기준 snapshot 정합 (의도된 동작).
 */
@Component
class UserRoleHierarchyEventHandler(
    private val hierarchyTraversal: UserRoleHierarchyTraversal,
    private val cacheManager: CacheManager,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUserRoleChanged(event: UserRoleChangedEvent) {
        try {
            handle(event)
        } catch (e: Exception) {
            log.error(
                "[user-role-hierarchy] event handler 실패 — userRoleId={}, changeType={}, message={} " +
                    "— snapshot 일시 stale 가능, 운영자가 recomputeAll() 수동 트리거로 복구",
                event.userRoleId, event.changeType, e.message, e,
            )
        }
    }

    private fun handle(event: UserRoleChangedEvent) {
        log.info(
            "[user-role-hierarchy] event 수신 userRoleId={}, changeType={}",
            event.userRoleId, event.changeType,
        )
        // recomputeSubtree 가 snapshot 재계산 + hierarchy cache evict 동시 수행
        hierarchyTraversal.recomputeSubtree(event.userRoleId)

        // memberGroupIds cache 추가 evict — Group evaluator 의 UserRole 의존 분기 정합
        evictCache(CacheConfig.CACHE_MEMBER_GROUP_IDS)
    }

    /**
     * 단일 cache 전체 evict (key 단위 evict 불가 — CacheManager 추상화 한계).
     *
     * Q3 옵션 1 (선택적 evict) 의 의도: 본 spec 단계는 cache 단위 evict 채택.
     * 사용자별 key 단위 evict 는 향후 정밀화 후보 (Redis SCAN 패턴 필요).
     */
    private fun evictCache(cacheName: String) {
        val cache = cacheManager.getCache(cacheName)
        if (cache == null) {
            log.debug("[user-role-hierarchy] cache 미존재 evict skip — {}", cacheName)
            return
        }
        cache.clear()
        log.debug("[user-role-hierarchy] cache evict — {}", cacheName)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserRoleHierarchyEventHandler::class.java)
    }
}
