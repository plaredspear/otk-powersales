package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.promotion.service.PPTMasterSapBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 전문행사조 마스터 SAP 송신 cron (Spec #765).
 *
 * 매시간 23분 KST (`0 23 * * * *`) 발화. 레거시 운영 CronTrigger `IF_REST_SAP_PPTMToSAP`
 * (`0 23 * * * ?` Asia/Seoul, WAITING) 와 정합.
 *
 * ## 주기 변경 경위 (2026-08-09)
 * 레거시는 동일 Apex 클래스 `IF_REST_SAP_PPTMToSAP.cls` 를 표시명만 달리해 복수 등록해 두었다
 * ("전문행사조 SAP 송신 배치" = 매일 12:00 / `IF_REST_SAP_PPTMToSAP` = 매시 23분, 그 외 2건은 PAUSED).
 * SAP `SD03300` 송신 클래스는 이 1개뿐이라 두 등록의 처리 내용은 완전히 동일하며, 주기만 다르다.
 * 이전에는 일 1회 등록에 맞춰 두었으나, 실제 가동 중인 매시간 등록 기준으로 정합을 되돌린다.
 *
 * 레거시 배치는 증분 전송이 아니다 — 매 실행마다 당월과 기간이 겹치는 확정 전문행사조 전건을
 * 재조회해 통째로 재전송한다 (전송이력 플래그/필터 없음). 신규 [PPTMasterSapBatchService.runDaily]
 * 도 동일하게 stateless 재조회 방식이라, 발화 주기를 높여도 중복 전송량만 늘 뿐 결과는 멱등이다.
 *
 * - `ScheduledJobRunner` 위임으로 실행 이력은 `scheduled_job_run` 테이블에 자동 적재
 * - `@SchedulerLock` 으로 다중 인스턴스 환경에서 중복 발화 방지
 * - 본문은 [PPTMasterSapBatchService.runDaily] 에 위임 (TeamMemberScheduleSapOutboundBatch 패턴 정합)
 * `app.sap.outbound.ppt-master.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@ConditionalOnProperty(name = ["app.sap.outbound.ppt-master.enabled"], havingValue = "true", matchIfMissing = false)
class PPTMasterSapOutboundBatch(
    private val pptMasterSapBatchService: PPTMasterSapBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.sap.outbound.ppt-master.cron:0 23 * * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            pptMasterSapBatchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "ppt-master-sap-batch"
    }
}
