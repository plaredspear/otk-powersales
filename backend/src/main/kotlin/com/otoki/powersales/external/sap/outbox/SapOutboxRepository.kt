package com.otoki.powersales.external.sap.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface SapOutboxRepository :
    JpaRepository<SapOutbox, Long>,
    SapOutboxRepositoryCustom {

    /**
     * 특정 도메인 aggregate 의 송신 row 중 주어진 상태가 하나라도 존재하는지 여부.
     * 주문 취소 경합 방어(Spec #597)에서 등록 outbox 의 in-flight(`PENDING`/`RETRY`) 여부 판정에 사용.
     */
    fun existsByDomainTypeAndAggregateIdAndStatusIn(
        domainType: String,
        aggregateId: Long,
        statuses: Collection<String>,
    ): Boolean
}
