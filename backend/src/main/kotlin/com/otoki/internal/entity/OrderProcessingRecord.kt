package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 주문 처리 현황 Entity (SAP 연동 데이터)
 * SAP 시스템에서 수신한 주문 처리 상태를 저장한다.
 */
@Entity
@Table(
    name = "order_processing_records",
    indexes = [
        Index(name = "idx_order_processing_records_order_id", columnList = "order_id")
    ]
)
class OrderProcessingRecord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(name = "sap_order_number", nullable = false, length = 20)
    val sapOrderNumber: String,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @Column(name = "delivered_quantity", nullable = false, length = 20)
    val deliveredQuantity: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    val deliveryStatus: DeliveryStatus = DeliveryStatus.WAITING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
