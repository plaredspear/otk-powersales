package com.otoki.powersales.sf.inbound.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * SF 인바운드 OAuth 인프라 health check.
 *
 * 본 endpoint 가 200 OK 를 반환하면 SF Apex 측에서 (1) 토큰 발급 → (2) Bearer 인증 → (3) scope 검증
 * 전체 흐름이 정상 동작함을 확인할 수 있다.
 *
 * 비즈니스 데이터 영향 0건 — 운영 health-check 용도로 영구 유지.
 */
@RestController
@RequestMapping("/api/v1/sf/inbound")
class SfInboundHealthController {

    @Operation(
        summary = "SF 인바운드 health check",
        description = "SF Apex 가 토큰 발급 + Bearer 인증 + sf.write scope 검증 전체 흐름이 정상 동작하는지 확인.",
        security = [SecurityRequirement(name = "Bearer")]
    )
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('SCOPE_sf.write')")
    fun health(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "OK",
                scope = "sf.write",
                timestamp = LocalDateTime.now()
            )
        )
    }

    data class HealthResponse(
        val status: String,
        val scope: String,
        val timestamp: LocalDateTime
    )
}
