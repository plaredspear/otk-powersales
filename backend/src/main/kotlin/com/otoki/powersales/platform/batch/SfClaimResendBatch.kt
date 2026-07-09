package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 전송실패(SEND_FAILED) 건 재전송 배치 — 제품클레임 + 물류클레임(제안) 통합 (기본 매시간 50분).
 *
 * 등록 시 SF 전송이 일시 장애(OAuth/네트워크/SF 순단)로 실패한 건을 주기적으로 재전송해 자동 복구한다.
 * 실제 재전송/상태전이/도메인 오케스트레이션은 [SfClaimResendBatchService.resendAll] 이 담당한다.
 *
 * 두 도메인을 한 잡에서 순차 처리해 SF 부하를 직렬화하고 운영 이력/토글을 단일화한다.
 * cron 은 기존 SF sync 배치들(정각/30분)과 겹치지 않게 50분에 둔다 (`app.batch.sf-resend.cron` 재정의 가능).
 *
 * 전역 `@EnableScheduling` (BatchConfig) 은 ON 이며 잡별 게이팅으로 제어한다.
 * `app.batch.sf-resend.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF — dev/prod `application.yml` 활성화).
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.sf-resend.enabled"], havingValue = "true", matchIfMissing = false)
class SfClaimResendBatch(
    private val sfClaimResendBatchService: SfClaimResendBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.sf-resend.cron:0 50 * * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            sfClaimResendBatchService.resendAll(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "sf-claim-resend"
    }
}
