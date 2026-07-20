package com.otoki.powersales.external.ovip.inbound.service

/**
 * OVIP 인바운드 scope 상수 — audit `scope` 컬럼 기록용 단일 출처.
 *
 * 컨트롤러의 `@PreAuthorize("hasAuthority('SCOPE_ovip.read')")` 와 같은 값을 가리킨다
 * (`@PreAuthorize` 는 SpEL 문자열이라 상수를 참조할 수 없어 리터럴이 중복된다 — 값 변경 시 양쪽을 함께 고친다).
 */
object OvipInboundScopes {
    const val READ = "ovip.read"
}
