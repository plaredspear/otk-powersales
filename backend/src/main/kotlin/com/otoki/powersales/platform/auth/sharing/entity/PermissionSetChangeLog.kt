package com.otoki.powersales.platform.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Spec #837 — PermissionSet 변경 이력 audit.
 *
 * SF SetupAuditTrail 동등 이상 (SF 는 6개월 기본 보존, 본 테이블은 영구). PS 의 메타/비트/삭제 모든 변경을
 * before/after JSON snapshot 으로 박제. PS 삭제 시 본 row 의 [permissionSetId] 는 ON DELETE SET NULL 로
 * NULL 처리되어 audit 은 보존됨.
 */
@Entity
@Table(name = "permission_set_change_log")
class PermissionSetChangeLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_change_log_id")
    val id: Long = 0,

    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    val eventType: PermissionSetChangeLogEventType,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot")
    val beforeSnapshot: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot")
    val afterSnapshot: String? = null,

    @Column(name = "changed_by_id", nullable = false)
    val changedById: Long,

    @Column(name = "changed_at", nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "change_reason", length = 500)
    val changeReason: String? = null,
)
