package com.otoki.powersales.sap.outbox

import org.springframework.stereotype.Component

/**
 * [SapOutboxStatusHandler] 들을 `domainType` 키로 lookup 하는 레지스트리 (Spec #592).
 *
 * 새 도메인 핸들러를 `@Component` 로 추가하면 자동 등록된다.
 */
@Component
class SapOutboxStatusHandlerRegistry(handlers: List<SapOutboxStatusHandler>) {

    private val byDomainType: Map<String, SapOutboxStatusHandler> =
        handlers.associateBy(SapOutboxStatusHandler::supports)

    fun resolve(domainType: String): SapOutboxStatusHandler? = byDomainType[domainType]
}
