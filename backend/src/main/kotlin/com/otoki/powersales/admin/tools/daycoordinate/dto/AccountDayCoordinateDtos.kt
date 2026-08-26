package com.otoki.powersales.admin.tools.daycoordinate.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 현재 적용 중인 이동매장 좌표 예외.
 *
 * @param customized Redis 저장값이 있는지 (false 면 코드 기본값이 적용 중)
 */
data class AccountDayCoordinateResponse(
    val externalKey: String,
    val dayOfWeek: String,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val customized: Boolean,
    val defaultDayOfWeek: String,
    val defaultLatitude: Double,
    val defaultLongitude: Double,
    val defaultLabel: String,
)

/**
 * 주소 → 좌표 변환 요청 (저장 전 미리보기).
 *
 * 좌표를 수기 입력하면 실제 영업 위치와 어긋난 값이 저장될 수 있고, 그 경우 해당 요일 출근등록이
 * 거리 초과로 전면 실패한다. 주소로 좌표를 확정한 뒤 저장하도록 변환 단계를 분리한다.
 */
data class GeocodeAccountDayCoordinateRequest(
    @field:NotBlank(message = "주소는 필수입니다")
    @field:Size(max = 200, message = "주소는 200자 이하여야 합니다")
    val address: String,
)

/**
 * 주소 → 좌표 변환 결과.
 *
 * 변환만 수행하고 저장하지 않는다 — 운영자가 좌표를 확인한 뒤 별도로 저장(POST) 한다.
 *
 * @param roadAddress / @param jibunAddress Naver 가 해석한 주소. 입력 주소가 의도한 장소로
 *        해석됐는지 운영자가 눈으로 대조하는 용도 (좌표 숫자만으로는 판별이 불가능하다).
 */
data class GeocodeAccountDayCoordinateResponse(
    val latitude: Double,
    val longitude: Double,
    val roadAddress: String?,
    val jibunAddress: String?,
)

/**
 * 좌표 예외 변경 요청.
 *
 * `latitude`/`longitude` 는 non-null 로 선언해 필드 누락 자체를 Jackson 역직렬화 단계에서 400 으로
 * 떨어뜨린다 (`Double?` + `@NotNull` 조합은 이후 강제 언랩을 부른다).
 */
data class UpdateAccountDayCoordinateRequest(
    @field:NotBlank(message = "요일은 필수입니다")
    val dayOfWeek: String,

    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val longitude: Double,

    /**
     * 장소 식별용 라벨 — 서버 로그(`ATT_GPS_DAY_COORDINATE_OVERRIDE`) 에 남는 유일한 추적 단서라
     * 필수로 받는다. 구분자(`|`)/개행은 저장 포맷과 로그를 깨므로 금지.
     */
    @field:NotBlank(message = "장소 라벨은 필수입니다")
    @field:Size(max = 50, message = "라벨은 50자 이하여야 합니다")
    @field:Pattern(regexp = "^[^\\r\\n|]*$", message = "라벨에 줄바꿈이나 | 문자는 사용할 수 없습니다")
    val label: String,
)
