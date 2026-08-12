package com.otoki.powersales.domain.activity.schedule.policy

import java.time.DayOfWeek

/**
 * 이동매장(요일별로 물리적 위치가 바뀌는 거래처) 의 출근등록 GPS 검증 좌표 예외 — **코드 기본값 정의**.
 *
 * 대상 거래처는 요일에 따라 다른 장소에서 영업하므로 `account.latitude/longitude` 1쌍만으로는
 * 거리 검증이 성립하지 않는다. 등록 시점의 요일이 예외에 매칭되면 그 좌표를 기준으로 검증한다.
 *
 * 실제 적용 값은 개발자 도구에서 Redis 로 덮어쓸 수 있다 —
 * 조회는 `AccountDayCoordinateOverrideStore` 를 통하고, 본 object 는 **Redis 키 부재/장애 시의
 * 폴백 기본값**과 요일 매칭 규칙만 제공한다. 직접 참조하지 말고 store 를 거칠 것.
 *
 * ## 레거시 매핑
 * SF Apex `Batch_JMartLatLong` — 대상 거래처(`ExternalKey__c='1015773'`) 1건을 조회해
 * 오늘 요일이 수요일이면 양구점 좌표, 금요일이면 원통점 좌표를 `Account.Latitude__c/Longitude__c`
 * 에 직접 `update` 하고, 그 외 요일은 아무 것도 하지 않는(빈 else) 일일 배치.
 *
 * ## 신규 차이
 * 1. **적재 → 조회**: Account 좌표를 덮어쓰지 않고 거리 검증 시점에만 예외 좌표를 적용한다.
 *    - `Account.latitude/longitude` 는 SF 동기 대상 컬럼(`@SFField`)이라 덮어쓴 값이 SF 로 역전파될 수 있고,
 *    - 배치가 실패하면 전날 좌표가 잔존해 하루 종일 잘못된 기준으로 검증되기 때문이다.
 * 2. **금요일(원통점) 예외 제외**: 현행 운영 기준으로 수요일 이동만 유효하다는 사용자 확인에 따름.
 * 3. **수요일 좌표 갱신**: 레거시 `38.101772/127.988819` → `38.1018113/127.9886619` (양구읍 청춘로 7).
 *
 * ## 확장 기준
 * 대상 거래처가 2건 이상으로 늘어나면 단일 키 구조(store)를 거래처별 다건으로 바꾼다.
 * 현재는 1건뿐이라 개발자 도구 화면도 거래처 고정 + 요일/좌표만 편집하는 형태로 둔다.
 */
object AccountDayCoordinateOverride {

    /** 요일별 좌표 예외 1건. */
    data class DayCoordinate(
        val dayOfWeek: DayOfWeek,
        val latitude: Double,
        val longitude: Double,
        val label: String,
    )

    /** 예외 대상 거래처 외부키 (`account.external_key`) — 제이마트 이동매장. */
    const val TARGET_EXTERNAL_KEY = "1015773"

    /** Redis 미설정 시 적용되는 코드 기본값 — 수요일 양구읍 청춘로 7. */
    val DEFAULT_COORDINATE = DayCoordinate(
        dayOfWeek = DayOfWeek.WEDNESDAY,
        latitude = 38.1018113,
        longitude = 127.9886619,
        label = "제이마트 양구점",
    )

    /**
     * 거래처 외부키가 예외 대상인지 판정한다 (공백 무시).
     */
    fun isTarget(externalKey: String?): Boolean =
        externalKey?.trim()?.takeIf { it.isNotEmpty() } == TARGET_EXTERNAL_KEY

    /**
     * 요일 문자열 → [DayOfWeek] (앞뒤 공백/대소문자 무시). 알 수 없는 값이면 null.
     *
     * `DayOfWeek` 는 JDK enum 이라 `fromNameOrNull` 을 붙일 수 없으므로 변환 책임을 여기로 모은다
     * (API 요청 파싱과 Redis 저장값 역직렬화가 같은 규칙을 쓰도록).
     */
    fun dayOfWeekOrNull(raw: String?): DayOfWeek? =
        raw?.trim()?.uppercase()?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
}
