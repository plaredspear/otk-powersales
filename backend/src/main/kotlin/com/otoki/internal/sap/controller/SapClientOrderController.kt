package com.otoki.internal.sap.controller

import com.otoki.internal.sap.dto.SapClientOrderRequest
import com.otoki.internal.sap.dto.SapSyncResponse
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.service.SapClientOrderService
import com.otoki.internal.sap.service.SapSyncLogService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/sap/client-order")
class SapClientOrderController(
    private val sapClientOrderService: SapClientOrderService,
    private val sapSyncLogService: SapSyncLogService
) {

    @PostMapping
    fun syncClientOrder(
        @RequestBody request: SapClientOrderRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SapSyncResponse> {
        val requestedAt = LocalDateTime.now()
        val startTime = System.currentTimeMillis()

        if (request.reqItemList.isEmpty()) {
            return ResponseEntity.ok(SapSyncResponse.empty())
        }

        return try {
            val result = sapClientOrderService.sync(request.reqItemList)
            val durationMs = System.currentTimeMillis() - startTime

            sapSyncLogService.log(
                apiName = "client-order",
                requestCount = request.reqItemList.size,
                result = result,
                durationMs = durationMs,
                requestIp = httpRequest.remoteAddr,
                requestedAt = requestedAt
            )

            ResponseEntity.ok(SapSyncResponse.success())
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime

            sapSyncLogService.log(
                apiName = "client-order",
                requestCount = request.reqItemList.size,
                result = SapSyncResult(successCount = 0, failCount = request.reqItemList.size),
                durationMs = durationMs,
                requestIp = httpRequest.remoteAddr,
                requestedAt = requestedAt
            )

            ResponseEntity.ok(SapSyncResponse.error("Transaction failed: ${e.message}"))
        }
    }
}
