package com.otoki.powersales.platform.auth.sharing.event

/**
 * UserRole entity 변경 이벤트.
 *
 * 발행: UserRole 을 변경하는 Service 가 `ApplicationEventPublisher.publishEvent` 로 명시 발행.
 *   (SF Stage1 CSV 적재는 `COPY FROM` raw SQL 경로 — entity listener 가 발화하지 않으므로
 *   `SfMigrationStage2Controller.runUserRoleHierarchyRecalc()` 가 batch 차원에서 재계산을 담당.)
 *
 * 수신: [com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyEventHandler] —
 *   `@TransactionalEventListener(AFTER_COMMIT)` 로 수신 후 hierarchy snapshot + cache 갱신.
 *
 * 트랜잭션 안전: AFTER_COMMIT phase 사용 → entity 변경이 commit 된 경우에만 snapshot 갱신
 * 트리거. rollback 시 snapshot 변경 0건 (data ↔ snapshot 정합 보장).
 *
 * ## changeType 별 핸들러 동작
 * - `CREATED` — 신규 UserRole 의 부모 subtree 재계산 (parentUserRoleId 의 subtree 에 포함됨)
 * - `UPDATED` — parent_user_role_id 변경 시 본인 subtree + 옛/새 부모 subtree 재계산
 * - `REMOVED` — 부모 subtree 의 자손 list 에서 본인 제거 + 본인 ancestor path 정리
 *
 * @param userRoleId 변경된 UserRole 의 PK
 * @param changeType 변경 종류 (CREATED / UPDATED / REMOVED)
 */
data class UserRoleChangedEvent(
    val userRoleId: Long,
    val changeType: ChangeType,
) {
    enum class ChangeType {
        CREATED,
        UPDATED,
        REMOVED,
    }
}
