package com.otoki.powersales.external.sap.outbox

import org.springframework.stereotype.Component

/**
 * SAP outbound interfaceId ↔ endpoint path 매핑 레지스트리 (Spec #592).
 *
 * 새 도메인 추가 시 [register] 호출로 endpoint 등록만 하면 [SapOutboxBatchService] 가 자동 dispatch.
 */
@Component
class SapInterfaceRegistry {

    private val endpoints = mutableMapOf<String, String>()

    fun register(interfaceId: String, endpointPath: String) {
        endpoints[interfaceId] = endpointPath
    }

    fun resolveEndpoint(interfaceId: String): String? = endpoints[interfaceId]

    fun isRegistered(interfaceId: String): Boolean = endpoints.containsKey(interfaceId)
}
