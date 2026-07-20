package com.otoki.powersales.domain.activity.schedule.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalTime

/**
 * 출근 등록 정책 (Spec #585).
 *
 * 거래처 위경도 vs 사원 현재 위치 허용 거리 + 출근등록 마감 시간. 운영 정책 변경 시 환경별 override 로 조정.
 *
 * - `gpsThresholdMeters` 기본값 500m (옵션: 200 / 500 / 1000).
 *   단위는 m (Q5). 레거시 `int km` 단위 한계(소수 km 표현 불가) 제거 목적.
 * - `registrationDeadline` 기본값 17:00 (서울 시각). 이 시각 이후 출근등록 차단.
 *   dev 환경은 테스트 편의를 위해 21:00 으로 override (application.yml dev document).
 */
@ConfigurationProperties(prefix = "app.attendance")
data class AttendanceProperties(
    val gpsThresholdMeters: Int = 500,
    val registrationDeadline: LocalTime = LocalTime.of(17, 0)
)
