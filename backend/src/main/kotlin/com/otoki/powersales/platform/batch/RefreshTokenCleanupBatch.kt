package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.auth.token.RefreshTokenStore
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 만료된 refresh token / family 무효화 행 정리.
 *
 * refresh 메타데이터를 Redis 에서 DB 로 옮기면서 사라진 **Redis TTL 자동 만료의 대체물**이다.
 * 유효성 판정 자체는 `expires_at > now` 로 이뤄지므로 본 배치가 늦어도 보안상 문제는 없고,
 * 순수하게 테이블 비대화만 막는다.
 *
 * 정상 회전 흐름에서는 갱신 때마다 이전 행이 삭제되어 사용자당 1행 수준으로 유지되며, 여기서
 * 남는 것은 로그아웃 없이 재로그인한 세션의 잔여 행과 퇴사/장기 미접속자 행(자동로그인 ON 은
 * 최대 60일)이다. 따라서 일 1회로 충분하다.
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(
    name = ["app.batch.refresh-token-cleanup.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class RefreshTokenCleanupBatch(
    private val refreshTokenStore: RefreshTokenStore,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(RefreshTokenCleanupBatch::class.java)

    @Scheduled(cron = "0 20 4 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    fun run() {
        try {
            scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
                val (tokens, revocations) = refreshTokenStore.deleteExpired()
                log.info("RefreshTokenCleanup 완료: tokens={}, familyRevocations={}", tokens, revocations)
                ctx.metadata(mapOf("deletedTokens" to tokens, "deletedFamilyRevocations" to revocations))
            }
        } catch (e: Exception) {
            log.error("RefreshTokenCleanup 실패", e)
        }
    }

    companion object {
        const val JOB_NAME = "refreshToken.cleanup"
    }
}
