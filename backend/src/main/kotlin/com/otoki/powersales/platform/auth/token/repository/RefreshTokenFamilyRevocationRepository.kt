package com.otoki.powersales.platform.auth.token.repository

import com.otoki.powersales.platform.auth.token.RefreshTokenAudience
import com.otoki.powersales.platform.auth.token.entity.RefreshTokenFamilyRevocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * Token Family 무효화 기록 Repository.
 */
interface RefreshTokenFamilyRevocationRepository : JpaRepository<RefreshTokenFamilyRevocation, Long> {

    fun findByAudienceAndFamilyId(
        audience: RefreshTokenAudience,
        familyId: String,
    ): RefreshTokenFamilyRevocation?

    /** 무효화 유효 여부 — `expiresAt` 경과분은 Redis TTL 만료와 동일하게 무효화 해제로 본다. */
    fun existsByAudienceAndFamilyIdAndExpiresAtAfter(
        audience: RefreshTokenAudience,
        familyId: String,
        now: LocalDateTime,
    ): Boolean

    /** 만료행 물리 정리 (Redis TTL 대체) — 배치 전용 bulk delete. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshTokenFamilyRevocation r WHERE r.expiresAt < :now")
    fun deleteExpired(@Param("now") now: LocalDateTime): Int
}
