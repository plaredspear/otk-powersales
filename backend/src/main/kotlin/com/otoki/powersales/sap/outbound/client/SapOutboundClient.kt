package com.otoki.powersales.sap.outbound.client

import com.otoki.powersales.sap.outbound.dto.SapOutboundRequest
import com.otoki.powersales.sap.outbound.dto.SapOutboundResponse

interface SapOutboundClient {
    fun send(path: String, request: SapOutboundRequest<*>): SapOutboundResponse
}
