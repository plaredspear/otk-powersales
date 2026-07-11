package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderRequestRepository : JpaRepository<OrderRequest, Long>, OrderRequestRepositoryCustom {

    fun findByClientRequestId(clientRequestId: String): OrderRequest?

    fun existsByOrderRequestNumber(orderRequestNumber: String): Boolean

    /**
     * 상태 전이 경합 방어용 행 잠금 조회 (Spec #597 보강).
     *
     * 주문 취소 커밋([com.otoki.powersales.domain.activity.order.service.OrderCancelCommitter])과
     * 등록 outbox 응답 반영([com.otoki.powersales.domain.activity.order.sap.handler.OrderRequestSapOutboxStatusHandler])이
     * 각각 별도 트랜잭션에서 `order_request_status` 를 갱신하므로, 두 경로가 동일 행에 `PESSIMISTIC_WRITE`
     * 락을 획득해 직렬화한다. 이로써 `CANCELED` 가 `APPROVED`/`SEND_FAILED` 로 덮이는 lost-update 를 차단.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRequest o where o.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): OrderRequest?
}
