package com.otoki.internal.sap.controller

import com.otoki.internal.sap.dto.SapClaimRequest
import com.otoki.internal.sap.dto.SapClaimResponse
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.service.SapClaimService
import com.otoki.internal.sap.service.SapSyncLogService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/sap/claim")
class SapClaimController(
    private val sapClaimService: SapClaimService,
    private val sapSyncLogService: SapSyncLogService
) {

    @PostMapping
    fun syncClaim(
        @RequestBody request: SapClaimRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SapClaimResponse> {
        val requestedAt = LocalDateTime.now()
        val startTime = System.currentTimeMillis()
        val claimItem = request.request

        if (claimItem == null) {
            return ResponseEntity.ok(SapClaimResponse.error("request is required"))
        }

        return try {
            val response = sapClaimService.syncClaim(claimItem)
            val durationMs = System.currentTimeMillis() - startTime

            val isSuccess = response.resultCode == "S"
            sapSyncLogService.log(
                apiName = "claim",
                requestCount = 1,
                result = SapSyncResult(
                    successCount = if (isSuccess) 1 else 0,
                    failCount = if (isSuccess) 0 else 1
                ),
                durationMs = durationMs,
                requestIp = httpRequest.remoteAddr,
                requestedAt = requestedAt
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime

            sapSyncLogService.log(
                apiName = "claim",
                requestCount = 1,
                result = SapSyncResult(successCount = 0, failCount = 1),
                durationMs = durationMs,
                requestIp = httpRequest.remoteAddr,
                requestedAt = requestedAt
            )

            ResponseEntity.ok(SapClaimResponse.error("처리 실패: ${e.message}"))
        }
    }
}
