/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/ **
 * 주문 제품 항목 Entity
 * 주문에 포함된 개별 제품 정보를 관리한다.
 * /
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

    @Column(name = "unit_price", nullable = false)
    val unitPrice: Long = 0,

    @Column(name = "amount", nullable = false)
    val amount: Long = 0,

    @Column(name = "pieces_per_box", nullable = false)
    val piecesPerBox: Int = 1,

    @Column(name = "min_order_unit", nullable = false)
    val minOrderUnit: Int = 1,

    @Column(name = "supply_quantity", nullable = false)
    val supplyQuantity: Int = 0,

    @Column(name = "dc_quantity", nullable = false)
    val dcQuantity: Int = 0,

    @Column(name = "is_cancelled", nullable = false)
    var isCancelled: Boolean = false,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "cancelled_by", length = 8)
    var cancelledBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    / **
     * 주문 항목을 취소한다.
     *
     * @param employeeId 취소 요청자 사번
     * /
    fun cancel(employeeId: String) {
        this.isCancelled = true
        this.cancelledAt = LocalDateTime.now()
        this.cancelledBy = employeeId
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
*/
