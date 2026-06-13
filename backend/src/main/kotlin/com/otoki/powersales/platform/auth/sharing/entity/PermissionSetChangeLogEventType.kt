package com.otoki.powersales.platform.auth.sharing.entity

/**
 * Spec #837 — PermissionSet 변경 이력 이벤트 유형.
 *
 * SF SetupAuditTrail 동등 audit. event_type 별로 beforeSnapshot / afterSnapshot 의 의미가 다르다:
 *  - [CREATE]      : before = null, after = 신규 PS 메타 + 빈 flags
 *  - [UPDATE_META] : before/after = label/description 변경 전후
 *  - [UPDATE_FLAGS]: before/after = system 비트 + objectPermissions + customPermissions 전체 본문
 *  - [DELETE]      : before = 삭제 직전 PS + flags + assignment 목록 snapshot, after = null
 */
enum class PermissionSetChangeLogEventType {
    CREATE,
    UPDATE_META,
    UPDATE_FLAGS,
    DELETE,
}
