package com.otoki.powersales.platform.auth.token.repository

import com.otoki.powersales.platform.auth.token.RefreshTokenAudience
import com.otoki.powersales.platform.auth.token.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * Refresh Token Repository.
 *
 * 모든 조회/삭제는 `audience` 를 조건에 포함한다 — MOBILE/WEB 의 `user_id` 는 서로 다른 id 공간이라
 * audience 없이 조회하면 두 채널이 충돌한다 ([RefreshTokenAudience] 참조).
 */
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    /**
     * 유효(미만료) 토큰 존재 여부 — 만료행이 정리 배치 전까지 잔존하므로 `expiresAt` 판정이 필수.
     *
     * **회전(rotation) 검증에는 쓰지 말 것** — 조회 후 삭제하는 2단계는 동시 요청에서 둘 다
     * 통과한다. 그 용도로는 [consume] 을 쓴다.
     */
    fun existsByAudienceAndTokenIdAndExpiresAtAfter(
        audience: RefreshTokenAudience,
        tokenId: String,
        now: LocalDateTime,
    ): Boolean

    /**
     * 유효 토큰을 **원자적으로 소비**(검증 + 회수) 하고 삭제된 행 수를 반환한다.
     *
     * 단일 SQL DELETE 이므로 동시 요청이 들어와도 DB 행 잠금으로 정확히 하나만 1 을 받고
     * 나머지는 0 을 받는다 — 0 이 곧 "이미 사용된 토큰"(재사용=탈취) 판정이다.
     * 파생 쿼리(`deleteBy...`) 는 SELECT 후 엔티티 단위 삭제라 이 보장을 주지 못하므로
     * bulk DELETE 로 작성한다.
     *
     * 영속성 컨텍스트를 비우지 않는다(`clearAutomatically` 미사용) — 호출부 트랜잭션이 이후
     * 로드/변경하는 엔티티(예: refresh 시 `employee.recordAppVersion` dirty checking)가
     * detach 되지 않도록 하기 위함. 본 쿼리는 RefreshToken 만 건드리고 호출부는 이 엔티티를
     * 로드하지 않으므로 1차 캐시 불일치 여지가 없다.
     */
    @Modifying
    @Query(
        "DELETE FROM RefreshToken t " +
            "WHERE t.audience = :audience AND t.tokenId = :tokenId AND t.expiresAt > :now"
    )
    fun consume(
        @Param("audience") audience: RefreshTokenAudience,
        @Param("tokenId") tokenId: String,
        @Param("now") now: LocalDateTime,
    ): Int

    /** 로그아웃 / 비밀번호 변경 / 단말 초기화 시 해당 채널의 사용자 세션 전량 회수. */
    fun deleteByAudienceAndUserId(audience: RefreshTokenAudience, userId: Long): Long

    /** 만료행 물리 정리 (Redis TTL 대체) — 배치 전용 bulk delete. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    fun deleteExpired(@Param("now") now: LocalDateTime): Int
}
