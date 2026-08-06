package com.otoki.powersales.domain.foundation.account.policy

/**
 * 거래처 좌표변환(Naver Geocode) 재시도 억제 정책.
 *
 * 실패를 두 부류로 나눠 다루며, **카운트 대상은 "주소 미확정" 뿐**이다:
 * - **주소 미확정** — 호출은 성공했으나 Naver 가 그 주소로 좌표를 확정하지 못함(`addresses` 비었거나 x/y 없음).
 *   같은 주소로 재호출해도 결과가 같으므로 `Account.geocodeFailCount` 를 증가시키고,
 *   [MAX_FAIL_COUNT] 에 도달하면 재조회 대상에서 제외한다.
 * - **일시 실패** — HTTP/네트워크/파싱 오류. 카운트하지 않는다. Naver 장애가 복구되면 그대로 재시도되어야
 *   하며, 카운트에 넣으면 장애 시간 동안 상한을 소진해 복구 후에도 영구 배제되는 역효과가 난다.
 *
 * 카운터는 좌표 조회 성공 시, 그리고 거래처 주소(`address1`) 변경 시 0 으로 초기화된다
 * (`AccountUpsertMapper` — SAP 인바운드 / `AccountUpdateTxService` — 웹 관리자 수정).
 *
 * 상한 소진 속도는 배치(하루 1회)와 출근등록 온디맨드 보강이 카운터를 공유하므로 일정하지 않다 —
 * 같은 거래처에 출근등록이 몰리면 하루 안에 상한에 닿을 수 있다. 같은 주소에 대한 Naver 응답이 하루
 * 사이에 달라지지 않아 실질 차이가 없다고 보고 단순한 공유 카운터를 택했다. 운영 피드백에 따라
 * "하루 1회만 증가" 규칙 추가를 재검토한다.
 */
object GeocodeRetryPolicy {

    /** 주소 미확정 누적 허용 횟수 — 이 값 이상이면 재조회 대상에서 제외. */
    const val MAX_FAIL_COUNT: Int = 3

    /** 재조회(배치 / 온디맨드 보강) 대상 여부. */
    fun isRetryable(failCount: Int): Boolean = failCount < MAX_FAIL_COUNT

    /** 재시도를 포기한 상태(운영 화면 "변환실패" 표기 기준). */
    fun isExhausted(failCount: Int): Boolean = !isRetryable(failCount)
}
