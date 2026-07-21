package com.otoki.powersales.external.ovip.inbound.service

/**
 * OVIP 인바운드 scope 상수 — audit `scope` 컬럼 기록용 단일 출처.
 *
 * scope 기반 인가(@PreAuthorize)는 제거되었으므로 이 값은 **인가가 아니라 감사 로그 표기 전용**이다.
 * 조회 요청이 성공하면 어떤 논리 scope 로 수신되었는지 로그에 남기기 위한 라벨 역할만 한다.
 */
object OvipInboundScopes {
    const val READ = "ovip.read"
}
