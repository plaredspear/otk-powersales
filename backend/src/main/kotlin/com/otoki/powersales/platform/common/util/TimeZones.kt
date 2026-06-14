package com.otoki.powersales.platform.common.util

import java.time.ZoneId

/**
 * 전사 timezone 상수 (스펙 #564 §3.4).
 *
 * 도메인 코드는 `ZoneId.of("Asia/Seoul")` 를 인라인하지 말고 본 상수를 사용한다.
 * - DB / JVM 시각은 UTC wall clock `LocalDateTime` 으로 보관 (전사 컨벤션)
 * - KST "오늘" 계산이 필요한 지점에서는 `LocalDate.now(SEOUL_ZONE)` 처럼 명시 사용
 */
object TimeZones {
    val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    val UTC_ZONE: ZoneId = ZoneId.of("UTC")
}
