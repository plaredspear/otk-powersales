package com.otoki.powersales.platform.auth.web.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * Web 관리자 대행 로그인 감사 로그 (Spec #851).
 *
 * 관리자가 다른 Web 사용자 계정을 대행(impersonation)한 시작/종료 이력을 적재한다.
 * 본 spec 범위는 적재 전용 — 조회 API/화면은 비범위 (Q5 옵션 1). 감사 가시성은 DB 직접 조회로 확보.
 *
 * - `adminUserId`: 실제 대행을 수행한 관리자 User PK
 * - `targetUserId`: 대행 대상 User PK
 * - `startedAt`: 대행 시작 시각
 * - `endedAt`: 대행 종료 시각. null = 미종료 (명시 종료 없이 만료된 세션 포함)
 * - `accessExpiresAt`: 발급한 대행 access 토큰의 만료 시각 (참고용)
 */
@DomainName("관리자대행로그인 감사로그")
@Entity
@Table(name = "impersonation_log")
class ImpersonationLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("대행로그인감사로그ID")
    @Column(name = "impersonation_log_id")
    val id: Long = 0,

    @FieldName("관리자사용자ID")
    @Column(name = "admin_user_id", nullable = false)
    val adminUserId: Long,

    @FieldName("대상사용자ID")
    @Column(name = "target_user_id", nullable = false)
    val targetUserId: Long,

    @FieldName("사유")
    @Column(name = "reason", length = 500)
    val reason: String? = null,

    @FieldName("대행시작시각")
    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,

    @FieldName("대행종료시각")
    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @FieldName("토큰만료시각")
    @Column(name = "access_expires_at", nullable = false)
    val accessExpiresAt: LocalDateTime,
) {
    /** 대행 종료 처리 — ended_at 갱신. */
    fun markEnded(endedAt: LocalDateTime) {
        this.endedAt = endedAt
    }
}
