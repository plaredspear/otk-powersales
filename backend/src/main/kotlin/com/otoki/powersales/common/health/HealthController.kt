package com.otoki.powersales.common.health

import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// /actuator/health 는 EB ALB 헬스체크 전용(VPC 내부 경로). 외부(웹/SAP)용 상태
// 엔드포인트는 /api/health 로 분리해 CloudFront path routing 규약에 맞춘다.
@RestController
@RequestMapping("/api")
class HealthController(private val healthEndpoint: HealthEndpoint) {

    data class Indicator(val status: String)
    data class Components(val db: Indicator?, val redis: Indicator?)
    data class HealthResponse(val status: String, val components: Components)

    @GetMapping("/health")
    fun health(): HealthResponse {
        val root = healthEndpoint.health()
        val composite = root as? CompositeHealthDescriptor
        return HealthResponse(
            status = root.status.code,
            components = Components(
                db = composite?.components?.get("db")?.let { Indicator(it.status.code) },
                redis = composite?.components?.get("redis")?.let { Indicator(it.status.code) },
            ),
        )
    }
}
