package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.policy.GeocodeRetryPolicy
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.naver.NaverGeocodeClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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
 * 단 판정 축은 **`address1` 단독** 으로 축소했다 — 좌표 query 는 `address1` 만 보내므로 `address2`
 * 변경으로 무효화해도 결과 좌표가 같아, 공백 구간만 늘리기 때문이다 (`legacy-deviation.md` §6).
 * 따라서 본 batch 의 "좌표 미수신" 후보에는 신규 거래처뿐 아니라 주소가 바뀐 기존 거래처도 포함된다.
 *
 * ## 배치 주기 공백 구제
 * 본 batch 는 매일 02시 1회 실행이라, SAP 인바운드가 주소 변경을 수신한 시점부터 다음 실행까지
 * 해당 거래처는 좌표가 없다. GPS 거리 검증이 필요한 출근 등록이 그 사이 전면 차단되므로,
 * [resolveCoordinatesOnDemand] 가 등록 시점 1회 즉시 보강으로 공백을 메운다.
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
     * 실패를 두 부류로 구분한다 ([GeocodeRetryPolicy]):
     * - **주소 미확정([GeocodeResult.ADDRESS_NOT_FOUND])**: Naver 가 그 주소로 좌표를 확정하지 못함
     *   (응답 `addresses` 비어있음 / x·y 없음). 주소가 바뀌지 않는 한 재시도해도 결과가 같으므로
     *   `geocodeFailCount` 를 증가시키고, 상한 도달 시 재조회 대상에서 제외한다(무한 재시도 억제).
     * - **일시 실패([GeocodeResult.CALL_FAILED])**: HTTP/네트워크/파싱 오류(`geocode()` 가 null).
     *   다음 실행에 성공할 수 있으므로 카운트하지 않고 재시도 대상으로 남긴다.
     *
     * @return 처리 결과 ([GeocodeResult]).
     */
    @Transactional
    fun enrichSingleAccount(accountId: Long): GeocodeResult {
        val account = accountRepository.findById(accountId).orElse(null) ?: run {
            log.warn("Account 조회 실패 — accountId={}", accountId)
            return GeocodeResult.CALL_FAILED
        }
        // 배치 후보 필터가 address1 non-null 을 보장하지만, 방어적으로 blank 도 주소 미확정으로 카운트한다
        // (조회할 주소가 없으면 재시도해도 동일 — 주소가 채워지면 주소 변경 훅이 카운터를 초기화).
        val address = account.address1
        if (address.isNullOrBlank()) {
            account.geocodeFailCount++
            return GeocodeResult.ADDRESS_NOT_FOUND
        }

        // geocode() == null → HTTP/네트워크/파싱 예외 (일시 실패). 카운트하지 않고 재시도에 맡긴다.
        val response = naverGeocodeClient.geocode(address) ?: return GeocodeResult.CALL_FAILED

        // 호출은 성공했으나 주소로 좌표를 찾지 못함 → 실패 횟수 증가.
        val first = response.addresses.firstOrNull()
        val x = first?.x
        val y = first?.y
        if (x.isNullOrBlank() || y.isNullOrBlank()) {
            account.geocodeFailCount++
            log.warn(
                "Naver Geocode 좌표 확정 실패 — accountId={} externalKey={} address={} failCount={}/{}",
                accountId, account.externalKey, address,
                account.geocodeFailCount, GeocodeRetryPolicy.MAX_FAIL_COUNT
            )
            return GeocodeResult.ADDRESS_NOT_FOUND
        }
        account.longitude = x
        account.latitude = y
        // 이전 실패 이력이 있어도(주소 변경으로 재진입한 경우 등) 성공 시 카운터 초기화.
        account.geocodeFailCount = 0
        return GeocodeResult.SUCCESS
    }

    /**
     * 실시간 경로에서 좌표가 비어 있을 때 **1회 즉시 보강** — 배치 주기(매일 02시) 공백 구제.
     *
     * SAP 인바운드가 주소 변경을 수신하면 [AccountUpsertMapper] 가 좌표를 null 로 무효화하고,
     * 실제 재취득은 다음 [com.otoki.powersales.platform.batch.AccountNaverGeocodeBatch] 실행까지 미뤄진다.
     * 그 사이 GPS 거리 검증이 필요한 기능(출근 등록)은 "거래처 좌표 없음"으로 전면 차단되므로,
     * 차단 직전에 본 메서드로 좌표를 즉시 확보해 정상 진행시킨다.
     *
     * [enrichSingleAccount] / [refreshSingleAccount] 와 다른 점:
     * - **호출자 트랜잭션과 분리**([Propagation.REQUIRES_NEW]) — 외부 HTTP 응답(최대 read 5s) 동안
     *   호출자의 쓰기 트랜잭션이 DB 커넥션을 점유하지 않도록. 보강 결과는 호출자 트랜잭션이
     *   이후 롤백되더라도 독립 커밋되어 남는다 (다음 요청이 재호출하지 않도록).
     * - **기존 좌표를 절대 null 로 덮어쓰지 않음** — 실시간 경로의 일시 오류가 데이터를 훼손하면 안 된다.
     * - **실패 상한([GeocodeRetryPolicy.MAX_FAIL_COUNT]) 도달 거래처는 호출 없이 즉시 포기** — 주소가
     *   바뀌지 않는 한 결과가 같으므로, 매 요청마다 외부 API 를 때리는 것을 막는다. 카운터는 배치와 공유한다.
     *
     * @return 확보한 좌표. 보강 불가(주소 부재 / 실패 상한 도달 / 호출 실패 / 좌표 미확정) 시 `null`.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resolveCoordinatesOnDemand(accountId: Long): Coordinates? {
        val account = accountRepository.findById(accountId).orElse(null) ?: return null

        // 이미 좌표가 있으면(동시 요청이 먼저 보강한 경우 등) 외부 호출 없이 그대로 사용.
        val currentLat = account.latitude
        val currentLng = account.longitude
        if (!currentLat.isNullOrBlank() && !currentLng.isNullOrBlank()) {
            return Coordinates(latitude = currentLat, longitude = currentLng)
        }

        // 실패 상한 도달 — 주소가 바뀔 때까지 재호출해도 결과가 같다.
        if (!GeocodeRetryPolicy.isRetryable(account.geocodeFailCount)) return null

        val address = account.address1
        if (address.isNullOrBlank()) return null

        val response = naverGeocodeClient.geocode(address) ?: run {
            // 일시 실패 — 마킹하지 않고 배치 재시도에 맡긴다. 기존 좌표(null)도 건드리지 않는다.
            log.warn("Naver Geocode 온디맨드 호출 실패 — accountId={} address={}", accountId, address)
            return null
        }

        val first = response.addresses.firstOrNull()
        val x = first?.x
        val y = first?.y
        if (x.isNullOrBlank() || y.isNullOrBlank()) {
            account.geocodeFailCount++
            log.warn(
                "Naver Geocode 온디맨드 좌표 확정 실패 — accountId={} address={} failCount={}/{}",
                accountId, address, account.geocodeFailCount, GeocodeRetryPolicy.MAX_FAIL_COUNT
            )
            return null
        }

        account.longitude = x
        account.latitude = y
        account.geocodeFailCount = 0
        log.info("ACCOUNT_GEOCODE_ON_DEMAND_RESOLVED accountId={} lat={} lng={}", accountId, y, x)
        return Coordinates(latitude = y, longitude = x)
    }

    /** 온디맨드 보강으로 확보한 거래처 좌표 (Account 저장 형식과 동일한 String). */
    data class Coordinates(val latitude: String, val longitude: String)

    /** 거래처 1건 좌표 보강 결과. */
    enum class GeocodeResult {
        /** 좌표 set 성공. */
        SUCCESS,

        /** 주소로 좌표를 확정하지 못함 — 영구 실패. `geocodeUnresolved` 마킹 대상. */
        ADDRESS_NOT_FOUND,

        /** HTTP/네트워크/파싱 오류 — 일시 실패. 마킹하지 않고 재시도. */
        CALL_FAILED,
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
            // 즉시 조회 1회 실패는 실패 횟수에 넣지 않는다 — 주소 미확정인지 일시 오류인지 구분하지 않고
            // 호출하는 경로라(`geocode()` 반환값 null 여부를 보지 않음) 일시 오류를 카운트할 위험이 있다.
            // 배치 재진입의 전제인 카운터 초기화는 호출자([AccountUpdateTxService.applyUpdate]) 가 주소
            // 변경 감지 시점에 수행한다. 배치가 재시도 후 여전히 못 찾으면 그때 카운터가 쌓인다.
            return
        }
        account.longitude = x
        account.latitude = y
        // 성공 시 실패 카운터 초기화(주소 변경 훅이 이미 초기화하지만 방어적으로 0 보장).
        account.geocodeFailCount = 0
    }

    /**
     * 좌표 미수신 거래처를 [limit] 건 조회 → 거래처별 보강.
     *
     * 실패는 주소 미확정 / 일시로 나눠 집계한다:
     * - `unresolved`: 주소로 좌표를 못 찾아 실패 횟수를 올린 건 (상한 도달 시 다음 배치부터 재조회 제외).
     * - `callFailed`: HTTP/네트워크/파싱 오류 등 일시 실패 (카운트하지 않음, 다음 배치 재시도 대상 유지).
     *
     * 조회 후보(`findCandidates`)에는 실패 상한([GeocodeRetryPolicy.MAX_FAIL_COUNT])에 도달한 거래처가
     * 제외되므로, 매 실행마다 재시도되는 건은 "아직 상한에 닿지 않은 신규/실패이력/일시실패" 뿐이다.
     *
     * @return 처리 결과 — `scanned` (조회 건수), `succeeded` (좌표 set 성공),
     *         `unresolved` (주소 미확정 — 실패 횟수 증가), `callFailed` (일시 실패)
     */
    fun enrichCoordinatesMissingAccounts(limit: Int): GeocodeBatchResult {
        val candidates = findCandidates(limit)
        if (candidates.isEmpty()) return GeocodeBatchResult(0, 0, 0, 0)

        var succeeded = 0
        var unresolved = 0
        var callFailed = 0
        for (candidate in candidates) {
            val result = try {
                enrichSingleAccount(candidate.id)
            } catch (ex: Exception) {
                log.warn(
                    "Account 좌표 보강 실패 — accountId={} externalKey={} cause={}",
                    candidate.id, candidate.externalKey, ex.message
                )
                GeocodeResult.CALL_FAILED
            }
            when (result) {
                GeocodeResult.SUCCESS -> succeeded++
                GeocodeResult.ADDRESS_NOT_FOUND -> unresolved++
                GeocodeResult.CALL_FAILED -> callFailed++
            }
        }
        return GeocodeBatchResult(
            scanned = candidates.size,
            succeeded = succeeded,
            unresolved = unresolved,
            callFailed = callFailed
        )
    }

    @Transactional(readOnly = true)
    internal fun findCandidates(limit: Int): List<Account> =
        accountRepository.findCoordinatesMissingAccounts(limit)

    data class GeocodeBatchResult(
        val scanned: Int,
        val succeeded: Int,
        /** 주소로 좌표를 못 찾아 실패 횟수를 올린 건 (상한 도달 시 다음 배치부터 재조회 제외). */
        val unresolved: Int,
        /** HTTP/네트워크/파싱 오류 등 일시 실패 (카운트하지 않음 — 다음 배치 재시도 대상 유지). */
        val callFailed: Int,
    )
}
