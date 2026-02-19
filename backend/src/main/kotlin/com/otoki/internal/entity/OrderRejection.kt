/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/ **
 * 주문 반려 제품 Entity
 * 마감 후 SAP에서 반려된 제품 정보를 저장한다.
 * /
@Entity
@Table(
    name = "order_rejections",
    indexes = [
        Index(name = "idx_order_rejections_order_id", columnList = "order_id")
    ]
)
class OrderRejection(

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

    @Column(name = "order_quantity_boxes", nullable = false)
    val orderQuantityBoxes: Int = 0,

    @Column(name = "rejection_reason", nullable = false, length = 500)
    val rejectionReason: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
*/
