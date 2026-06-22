package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.naver.NaverGeocodeClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 좌표 미수신 거래처를 Naver Cloud Map Geocode API 로 보강하는 service (#637).
 *
 * ## 레거시 매핑
 * - SF Apex: `Batch_AccountLatLong.cls#start` / `#execute` (force-app/main/default/classes/Batch_AccountLatLong.cls)
 * - flow-legacy: flow-legacy yaml (Spec #637)
 * - origin spec: #637
 *
 * ## 레거시 동작 요약
 * 1. 입력: `(latitude=null OR longitude=null) AND address1!=null AND external_key!=null AND account_status_name='거래'` 거래처 1000건
 * 2. 거래처별 직렬 HTTP GET 호출 → Naver API (`https://maps.apigw.ntruss.com/map-geocode/v2/geocode`)
 * 3. 응답 `addresses[0].x` → `longitude` (String), `addresses[0].y` → `latitude` (String) set
 * 4. 부수 효과: `TriggerHandler.bypass('ClientMasterReceiver')` 후 `Database.update(accList, false)` partial-success
 * 5. 분기: HTTP/JSON 예외 catch → `IF_Util.setLog('IF_Util_updateCoords', ..., 'outbound', ...)`
 *
 * ## 신규 차이
 * - DML 실패 audit 미적재 — slf4j WARN + ScheduledJobRunner metadata 의 `failed` 카운터로 대체.
 *   거래처별 단일 트랜잭션 (JPA dirty-checking) 이라 partial-success 적재 단위가 부재. 좌표 null 잔존이 다음 batch 자연 재진입의 신호.
 *   참조: `legacy-deviation.md` §7 시스템·인프라 ("legacy partial-success DML audit → slf4j + ScheduledJobRun metadata 대체")
 * - Trigger bypass 자연 소멸 — 신규 시스템에 SF Trigger 부재.
 *
 * ## 좌표 재취득 트리거 (주소 변경)
 * 레거시는 거래처 마스터 수신 시 주소(Address1/Address2)가 변경되면 trigger(setLatLongNull)가
 * 좌표를 null 로 초기화해 본 batch 후보로 자연 재진입시킨다. 신규는 SF Trigger 가 없으므로
 * 그 책임을 [AccountUpsertMapper.update] 가 대신 진다 (주소 변경 감지 → latitude/longitude null).
 * 따라서 본 batch 의 "좌표 미수신" 후보에는 신규 거래처뿐 아니라 주소가 바뀐 기존 거래처도 포함된다.
 */
@Service
class AccountNaverGeocodeService(
    private val accountRepository: AccountRepository,
    private val naverGeocodeClient: NaverGeocodeClient
) {

    private val log = LoggerFactory.getLogger(AccountNaverGeocodeService::class.java)

    /**
     * 좌표 미수신 거래처 1건 보강 — 본 메서드 단위로 트랜잭션 분리.
     *
     * dirty checking 으로 트랜잭션 commit 시 자동 UPDATE.
     *
     * @return 좌표 set 성공 여부 (응답에 `addresses` 가 비어 있거나 호출 실패 시 false)
     */
    @Transactional
    fun enrichSingleAccount(accountId: Long): Boolean {
        val account = accountRepository.findById(accountId).orElse(null) ?: run {
            log.warn("Account 조회 실패 — accountId={}", accountId)
            return false
        }
        val address = account.address1 ?: return false

        val response = naverGeocodeClient.geocode(address) ?: return false
        val first = response.addresses.firstOrNull() ?: run {
            log.warn("Naver Geocode 응답에 addresses 없음 — accountId={} externalKey={}", accountId, account.externalKey)
            return false
        }
        val x = first.x
        val y = first.y
        if (x.isNullOrBlank() || y.isNullOrBlank()) {
            log.warn(
                "Naver Geocode 응답 좌표 null — accountId={} externalKey={} x={} y={}",
                accountId, account.externalKey, x, y
            )
            return false
        }
        account.longitude = x
        account.latitude = y
        return true
    }

    /**
     * 거래처 1건의 좌표를 `address1` 으로 즉시 재조회 — web admin 거래처 주소 수정 후행 처리.
     *
     * [enrichSingleAccount] 와 좌표 조회/적용 로직은 동일하되, **조회 실패 / 응답 좌표 부재 시
     * 기존 좌표를 null 로 무효화**한다는 점이 다르다. 거래처 수정으로 주소가 바뀐 상황에서는
     * 옛 좌표가 잔존하면 안 되며, 무효화해야 본 service 의 보강 배치 후보(latitude/longitude IS NULL)
     * 로 재진입해 후속 보강할 수 있다. ([enrichSingleAccount] 는 좌표 미수신 거래처 보강이 목적이라
     * 실패 시 좌표를 건드리지 않고 다음 배치 재시도에 맡긴다.)
     *
     * 본 메서드 단위로 트랜잭션을 분리한다 — 거래처 수정 메인 쓰기 트랜잭션이 외부 HTTP 응답 동안
     * DB 커넥션을 점유하지 않도록 [AccountUpdateService.update] 커밋 후 후행 호출된다.
     *
     * `address1` 이 없으면(null/blank) 조회할 주소가 없으므로 좌표를 null 로 무효화한다.
     */
    @Transactional
    fun refreshSingleAccount(accountId: Long) {
        val account = accountRepository.findById(accountId).orElse(null) ?: run {
            log.warn("Account 조회 실패 — accountId={}", accountId)
            return
        }
        val address = account.address1
        if (address.isNullOrBlank()) {
            account.latitude = null
            account.longitude = null
            return
        }

        val first = naverGeocodeClient.geocode(address)?.addresses?.firstOrNull()
        val x = first?.x
        val y = first?.y
        if (x.isNullOrBlank() || y.isNullOrBlank()) {
            log.warn("거래처 주소 변경 좌표 조회 실패 — accountId={} address={}", accountId, address)
            account.latitude = null
            account.longitude = null
            return
        }
        account.longitude = x
        account.latitude = y
    }

    /**
     * 좌표 미수신 거래처를 [limit] 건 조회 → 거래처별 보강.
     *
     * @return 처리 결과 — `scanned` (조회 건수), `succeeded` (좌표 set 성공), `failed` (좌표 set 실패)
     */
    fun enrichCoordinatesMissingAccounts(limit: Int): GeocodeBatchResult {
        val candidates = findCandidates(limit)
        if (candidates.isEmpty()) return GeocodeBatchResult(0, 0, 0)

        var succeeded = 0
        var failed = 0
        for (candidate in candidates) {
            val ok = try {
                enrichSingleAccount(candidate.id)
            } catch (ex: Exception) {
                log.warn(
                    "Account 좌표 보강 실패 — accountId={} externalKey={} cause={}",
                    candidate.id, candidate.externalKey, ex.message
                )
                false
            }
            if (ok) succeeded++ else failed++
        }
        return GeocodeBatchResult(scanned = candidates.size, succeeded = succeeded, failed = failed)
    }

    @Transactional(readOnly = true)
    internal fun findCandidates(limit: Int): List<Account> =
        accountRepository.findCoordinatesMissingAccounts(limit)

    data class GeocodeBatchResult(
        val scanned: Int,
        val succeeded: Int,
        val failed: Int
    )
}
