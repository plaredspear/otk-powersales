package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import java.time.DayOfWeek

/**
 * 이동매장(요일별로 물리적 위치가 바뀌는 거래처) 의 출근등록 GPS 검증 좌표 오버라이드.
 *
 * 대상 거래처는 요일에 따라 다른 장소에서 영업하므로 `account.latitude/longitude` 1쌍만으로는
 * 거리 검증이 성립하지 않는다. 등록 시점의 요일이 등록된 예외에 매칭되면 그 좌표를 기준으로 검증한다.
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
 * 매칭되는 요일이 없으면 `null` 을 반환해 호출자가 기존 `account` 좌표 경로를 그대로 타게 한다.
 *
 * ## 확장 기준
 * 대상이 3건 이상 누적되면 `@ConfigurationProperties` 또는 마스터 테이블로 외부화한다.
 * 현재는 1건뿐이라 배포 없는 운영 조정 이점보다 코드 자족성이 크다고 판단해 상수로 둔다.
 */
object AccountDayCoordinateOverride {

    /** 요일별 좌표 예외 1건. */
    data class DayCoordinate(
        val dayOfWeek: DayOfWeek,
        val latitude: Double,
        val longitude: Double,
        val label: String,
    )

    /**
     * 거래처 외부키(`account.external_key`) → 요일별 좌표 예외.
     *
     * 제이마트(1015773): 수요일에 양구읍 청춘로 7 로 이동 영업. 그 외 요일은 예외 없음(원본 좌표 사용).
     */
    private val OVERRIDES: Map<String, List<DayCoordinate>> = mapOf(
        "1015773" to listOf(
            DayCoordinate(DayOfWeek.WEDNESDAY, 38.1018113, 127.9886619, "제이마트 양구점"),
        ),
    )

    /**
     * 해당 거래처 / 요일에 적용할 좌표 예외를 조회한다.
     *
     * @return 매칭된 예외 좌표. 대상 거래처가 아니거나 해당 요일 예외가 없으면 null.
     */
    fun resolve(account: Account?, dayOfWeek: DayOfWeek): DayCoordinate? {
        val externalKey = account?.externalKey?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return OVERRIDES[externalKey]?.firstOrNull { it.dayOfWeek == dayOfWeek }
    }
}
