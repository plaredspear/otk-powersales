package com.otoki.powersales.platform.auth.token.entity

import com.otoki.powersales.platform.auth.token.RefreshTokenAudience
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Token Family 무효화 기록 — refresh token 재사용(탈취) 감지 시 해당 family 의 후속 갱신을 전부 차단한다.
 *
 * Redis `refresh_family:<familyId>` = "revoked" + TTL 의 대체물. `expires_at` 경과 시 무효화가
 * 자연 해제되는 것도 Redis TTL 동작과 동일하다 — 그 시점엔 같은 family 의 refresh JWT 도 이미
 * 만료되어 있으므로 차단을 유지할 이유가 없다.
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.platform.common.salesforce.SFObject]
 * 어노테이션을 두지 않는다.
 */
@DomainName("리프레시토큰패밀리무효화")
@Entity
@Table(name = "refresh_token_family_revocation")
class RefreshTokenFamilyRevocation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("리프레시토큰패밀리무효화ID")
    @Column(name = "refresh_token_family_revocation_id")
    val id: Long = 0,

    @FieldName("발급채널")
    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 10)
    val audience: RefreshTokenAudience,

    @FieldName("토큰패밀리ID")
    @Column(name = "family_id", nullable = false, length = 36)
    val familyId: String,

    @FieldName("무효화일시")
    @Column(name = "revoked_at", nullable = false)
    val revokedAt: LocalDateTime,

    @FieldName("만료일시")
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

) : BaseEntity() {

    /**
     * 이미 무효화된 family 가 다시 무효화 요청을 받은 경우 차단 만료만 연장한다.
     * (UNIQUE(audience, family_id) 제약 위반 없이 재-revoke 를 멱등 처리)
     */
    fun extendUntil(newExpiresAt: LocalDateTime) {
        if (newExpiresAt.isAfter(this.expiresAt)) {
            this.expiresAt = newExpiresAt
        }
    }
}
