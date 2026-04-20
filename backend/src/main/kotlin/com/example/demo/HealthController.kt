package com.example.demo

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// /actuator/health 는 EB ALB 헬스체크 전용(VPC 내부 경로). 외부(웹/SAP)용 상태
// 엔드포인트는 /api/health 로 분리해 CloudFront path routing 규약에 맞춘다.
//
// DB/Redis 연결이 현재 단계에서 제외되어 있으므로 components 필드는 비워둔다.
// 향후 복구 시 CompositeHealthDescriptor 로 캐스팅해 db/redis 상태를
// 다시 노출하도록 확장한다.
@RestController
@RequestMapping("/api")
class HealthController(private val healthEndpoint: HealthEndpoint) {

    data class HealthResponse(val status: String)

    @GetMapping("/health")
    fun health(): HealthResponse {
        val root = healthEndpoint.health()
        return HealthResponse(status = root.status.code)
    }
}
