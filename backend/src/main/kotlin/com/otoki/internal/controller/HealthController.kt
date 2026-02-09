package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Health Check API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    @GetMapping
    fun health(): ApiResponse<HealthStatus> {
        val healthStatus = HealthStatus(
            status = "UP",
            timestamp = LocalDateTime.now(),
            version = "0.0.1-SNAPSHOT"
        )

        return ApiResponse.success(
            data = healthStatus,
            message = "서버가 정상적으로 실행 중입니다"
        )
    }
}

/**
 * Health Check 응답 DTO
 */
data class HealthStatus(
    val status: String,
    val timestamp: LocalDateTime,
    val version: String
)
