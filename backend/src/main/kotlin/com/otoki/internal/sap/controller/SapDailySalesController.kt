package com.otoki.internal.sap.controller

import com.otoki.internal.sap.dto.SapDailySalesRequest
import com.otoki.internal.sap.dto.SapSyncResponse
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.service.SapDailySalesService
import com.otoki.internal.sap.service.SapSyncLogService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/sap/daily-sales")
class SapDailySalesController(
    private val sapDailySalesService: SapDailySalesService,
    private val sapSyncLogService: SapSyncLogService
) {

    @PostMapping
    fun syncDailySales(
        @RequestBody request: SapDailySalesRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SapSyncResponse> {
        val requestedAt = LocalDateTime.now()
        val startTime = System.currentTimeMillis()

        if (request.reqItemList.isEmpty()) {
            return ResponseEntity.ok(SapSyncResponse.empty())
        }

        return try {
            val result = sapDailySalesService.sync(request.reqItemList)
            val durationMs = System.currentTimeMillis() - startTime

            sapSyncLogService.log(
                apiName = "daily-sales",
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
                apiName = "daily-sales",
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
