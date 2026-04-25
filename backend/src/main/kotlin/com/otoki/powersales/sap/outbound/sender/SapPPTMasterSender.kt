package com.otoki.powersales.sap.outbound.sender

import com.otoki.powersales.promotion.repository.PPTMasterRepository
import com.otoki.powersales.sap.outbound.client.SapOutboundClient
import com.otoki.powersales.sap.outbound.dto.SapOutboundRequest
import com.otoki.powersales.sap.outbound.dto.SapPPTMasterOutboundDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

@Component
class SapPPTMasterSender(
    private val pptMasterRepository: PPTMasterRepository,
    private val sapOutboundClient: SapOutboundClient,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    @Transactional(readOnly = true)
    fun send(): SapPPTMasterSendResult {
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val masters = pptMasterRepository.findSapOutboundTargets(
            month.atDay(1),
            month.atEndOfMonth()
        )

        if (masters.isEmpty()) {
            val response = sapOutboundClient.send(
                ENDPOINT_PATH,
                SapOutboundRequest(interfaceId = INTERFACE_ID, reqItemList = emptyList<SapPPTMasterOutboundDto>())
            )
            return SapPPTMasterSendResult(
                requestCount = 0,
                batchCount = 0,
                resultCode = response.resultCode,
                resultMsg = response.resultMsg
            )
        }

        val items = masters.map { SapPPTMasterOutboundDto.from(it, today) }
        val batches = items.chunked(BATCH_SIZE)
        var lastResponseCode = ""
        var lastResponseMsg = ""

        for (batch in batches) {
            val response = sapOutboundClient.send(
                ENDPOINT_PATH,
                SapOutboundRequest(interfaceId = INTERFACE_ID, reqItemList = batch)
            )
            lastResponseCode = response.resultCode
            lastResponseMsg = response.resultMsg
        }

        return SapPPTMasterSendResult(
            requestCount = items.size,
            batchCount = batches.size,
            resultCode = lastResponseCode,
            resultMsg = lastResponseMsg
        )
    }

    companion object {
        const val INTERFACE_ID: String = "SD03300"
        const val ENDPOINT_PATH: String = "SD03300"
        const val BATCH_SIZE: Int = 100
    }
}

data class SapPPTMasterSendResult(
    val requestCount: Int,
    val batchCount: Int,
    val resultCode: String,
    val resultMsg: String
)
