package com.otoki.powersales.sap.outbound.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sap.outbound.sender.SapPPTMasterSender
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/v1/admin/sap/outbound")
class SapOutboundController(
    private val sapPPTMasterSender: SapPPTMasterSender
) {

    @PostMapping("/SD03300/send")
    fun sendPPTMaster(): ResponseEntity<ApiResponse<SapOutboundSendResponse>> {
        val startNanos = System.nanoTime()
        val result = sapPPTMasterSender.send()
        val durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis()

        return ResponseEntity.ok(
            ApiResponse.success(
                SapOutboundSendResponse(
                    interfaceId = "SD03300",
                    requestCount = result.requestCount,
                    batchCount = result.batchCount,
                    resultCode = result.resultCode,
                    resultMsg = result.resultMsg,
                    durationMs = durationMs
                ),
                "SAP 송신 완료"
            )
        )
    }
}

data class SapOutboundSendResponse(
    val interfaceId: String,
    val requestCount: Int,
    val batchCount: Int,
    val resultCode: String,
    val resultMsg: String,
    val durationMs: Long
)
