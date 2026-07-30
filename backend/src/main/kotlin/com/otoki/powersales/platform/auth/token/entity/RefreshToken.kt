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
 * Refresh Token 메타데이터 (Rotation 의 source of truth).
 *
 * 행의 **존재 여부가 곧 "아직 사용되지 않은 유효 토큰"** 을 뜻한다. 갱신 시 이전 행을 삭제하고
 * 새 행을 넣으므로, 이미 삭제된 tokenId 로 갱신이 들어오면 재사용(=탈취) 으로 판정해
 * [RefreshTokenFamilyRevocation] 으로 family 전체를 차단한다. 이 의미론을 깨면 탈취 감지가
 * 통째로 무력화되므로, 이력 보관 목적으로 행을 남기려면 반드시 별도 상태 컬럼을 도입해야 한다.
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.platform.common.salesforce.SFObject]
 * 어노테이션을 두지 않는다.
 */
@DomainName("리프레시토큰")
@Entity
@Table(name = "refresh_token")
class RefreshToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("리프레시토큰ID")
    @Column(name = "refresh_token_id")
    val id: Long = 0,

    @FieldName("발급채널")
    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 10)
    val audience: RefreshTokenAudience,

    @FieldName("토큰ID")
    @Column(name = "token_id", nullable = false, length = 36)
    val tokenId: String,

    /** 채널별 의미가 다르다 — MOBILE=employee_id / WEB=users_id. [RefreshTokenAudience] 참조. */
    @FieldName("사용자ID")
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @FieldName("토큰패밀리ID")
    @Column(name = "family_id", nullable = false, length = 36)
    val familyId: String,

    @FieldName("발급일시")
    @Column(name = "issued_at", nullable = false)
    val issuedAt: LocalDateTime,

    /** Redis TTL 의 대체물. 조회 시 `expires_at > now` 를 반드시 함께 판정한다 (만료행이 배치 전까지 잔존). */
    @FieldName("만료일시")
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

) : BaseEntity()
