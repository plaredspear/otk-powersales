package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.service.AdminClaimMasterSyncTestService
import com.otoki.powersales.domain.activity.claim.service.AdminLogisticsClaimMasterSyncTestService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * SF 클레임/물류클레임 상태 업데이트(inbound sync) 배치 실행 서비스 — 두 도메인 통합.
 *
 * 두 도메인 모두 `SF fetch(MOD_DT=오늘) → pwrskey 매칭 → 조치/상담 필드 갱신` 으로 구조가 동형이고 동일 SF 서버
 * (`SfOutboundClient` / OAuth 토큰) 를 공유하므로, 한 잡에서 순차 처리해 SF 부하를 직렬화하고 운영 이력/토글을
 * 단일화한다(SF 재전송 배치([SfClaimResendBatchService]) 와 동일 패턴).
 *
 * 도메인 간 실패는 [runCatching] 으로 격리 — 한 도메인의 SF 호출/파싱/갱신 예외가 다른 도메인 처리를 막지 않는다.
 * 개별 도메인 실패는 metadata `error` 플래그로 이력에 남긴다(개별 sync 서비스는 배치 경로에서 SF 실패를
 * throw 하지만, 통합 잡은 도메인 격리를 우선하고 실패를 가시화한다).
 *
 * ## 도메인별 개별 on/off (기존 분리 제어 유지)
 * 잡 빈 자체는 `app.batch.claim-master.sync.enabled` 로 게이팅되지만, 통합 후에도 claim / logistics 를 따로 켜고
 * 끄던 운영 제어를 보존하기 위해 각 도메인 처리 여부를 개별 플래그로 다시 판정한다:
 *  - claim: `app.batch.claim-master.sync.enabled`
 *  - logistics: `app.batch.logistics-claim-master.sync.enabled`
 * 꺼진 도메인은 SF 호출 없이 skip 되고 metadata 에 `enabled=false` 로 기록된다. (잡 빈 게이팅이 claim 플래그라,
 * claim 만 꺼두면 잡은 발화하되 claim 도메인만 skip — 개별 제어 그대로.)
 *
 * 개별 전송/갱신은 각 도메인 서비스에 위임한다(개발자도구 "외부 API 테스트" 화면이 같은 서비스를 개별 호출하므로
 * 서비스는 병합하지 않는다):
 *  - 클레임: [AdminClaimMasterSyncTestService.sync]
 *  - 물류클레임: [AdminLogisticsClaimMasterSyncTestService.sync]
 */
@Service
class ClaimMasterSyncBatchService(
    private val claimSyncService: AdminClaimMasterSyncTestService,
    private val logisticsSyncService: AdminLogisticsClaimMasterSyncTestService,
    @Value("\${app.batch.claim-master.sync.enabled:false}") private val claimEnabled: Boolean,
    @Value("\${app.batch.logistics-claim-master.sync.enabled:false}") private val logisticsEnabled: Boolean,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun syncAll(context: ScheduledJobRunContext? = null) {
        val claim = runDomain(DOMAIN_CLAIM, claimEnabled) { claimSyncService.sync().toDomainResult() }
        val logistics = runDomain(DOMAIN_LOGISTICS, logisticsEnabled) { logisticsSyncService.sync().toDomainResult() }

        log.info(
            "CLAIM_MASTER_SYNC_BATCH claim({}) logistics({})",
            claim.toLogString(), logistics.toLogString(),
        )
        context?.metadata(
            mapOf(
                DOMAIN_CLAIM to claim.toMetadata(),
                DOMAIN_LOGISTICS to logistics.toMetadata(),
            )
        )
    }

    private fun runDomain(domain: String, enabled: Boolean, block: () -> DomainResult): DomainResult {
        if (!enabled) {
            return DomainResult.disabled()
        }
        return runCatching(block)
            .onFailure { log.warn("[claim-master-sync] {} 도메인 sync 실패", domain, it) }
            .getOrDefault(DomainResult.error())
    }

    /** [AdminClaimMasterSyncTestService.UpdateResult] 정합 — 도메인별 갱신 집계. */
    private data class DomainResult(
        val fetched: Int,
        val updated: Int,
        val notFound: Int,
        val skipped: Int,
        val enabled: Boolean = true,
        val error: Boolean = false,
    ) {
        fun toMetadata(): Map<String, Any?> = mapOf(
            "enabled" to enabled,
            "error" to error,
            "fetched" to fetched,
            "updated" to updated,
            "notFound" to notFound,
            "skipped" to skipped,
        )

        fun toLogString(): String =
            "enabled=$enabled error=$error fetched=$fetched updated=$updated notFound=$notFound skipped=$skipped"

        companion object {
            fun disabled() = DomainResult(0, 0, 0, 0, enabled = false)
            fun error() = DomainResult(0, 0, 0, 0, error = true)
        }
    }

    private fun AdminClaimMasterSyncTestService.UpdateResult.toDomainResult() =
        DomainResult(fetched = fetched, updated = updated, notFound = notFound, skipped = skipped)

    private fun AdminLogisticsClaimMasterSyncTestService.UpdateResult.toDomainResult() =
        DomainResult(fetched = fetched, updated = updated, notFound = notFound, skipped = skipped)

    companion object {
        /** metadata map key 겸 로그 라벨 — claim 도메인. */
        const val DOMAIN_CLAIM = "claim"

        /** metadata map key 겸 로그 라벨 — logistics(물류클레임/제안) 도메인. */
        const val DOMAIN_LOGISTICS = "logistics"
    }
}
