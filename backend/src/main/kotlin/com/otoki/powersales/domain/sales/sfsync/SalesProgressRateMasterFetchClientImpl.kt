package com.otoki.powersales.domain.sales.sfsync

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [SalesProgressRateMasterFetchClient] 운영 구현 — **SF 통신부는 미구현 (TODO)**.
 *
 * 현재는 빈 리스트를 반환하여 sync 배치가 no-op 으로 안전하게 동작한다. SF endpoint/인증이
 * 확정되면 아래 TODO 를 채운다. upsert 로직([SalesProgressRateMasterSyncService])은 이 client 가
 * 반환하는 DTO 리스트에만 의존하므로, client 완성 전에도 upsert 경로는 독립적으로 검증 가능하다.
 */
@Component
class SalesProgressRateMasterFetchClientImpl : SalesProgressRateMasterFetchClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(): List<SalesProgressRateMasterFetchDto> {
        // TODO(SF-fetch): SF `SalesProgressRateMaster__c` 변경분 fetch 구현.
        //   - endpoint: SF Apex REST 또는 Query API URL (예: sf.outbound.apex-base-url + suffix)
        //   - 인증: SfOAuthTokenManager.getAccessToken() Bearer 헤더 (SfOutboundClientImpl 패턴 참조)
        //   - 조회 조건: SF 측이 LastModifiedDate 기준 변경분을 응답 (증분 파라미터 전달 여부는 API 확정 후)
        //   - 페이지네이션: SF nextRecordsUrl 처리 (대량 응답 대비)
        //   - 매핑: SF 응답 JSON → SalesProgressRateMasterFetchDto (필드명: TargetYear__c 등)
        log.warn("[sales-progress-sync] SF fetch client 미구현 — 빈 리스트 반환 (no-op)")
        return emptyList()
    }
}
