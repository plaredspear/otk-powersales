package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 주문 제품 항목 Entity
 * 주문에 포함된 개별 제품 정보를 관리한다.
 */
@Entity
@Table(
    name = "order_items",
    indexes = [
        Index(name = "idx_order_items_order_id", columnList = "order_id")
    ]
)
class OrderItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @Column(name = "quantity_boxes", nullable = false)
    val quantityBoxes: Double = 0.0,

    @Column(name = "quantity_pieces", nullable = false)
    val quantityPieces: Int = 0,

    @Column(name = "is_cancelled", nullable = false)
    val isCancelled: Boolean = false,

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
