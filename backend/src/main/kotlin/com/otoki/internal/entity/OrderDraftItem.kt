package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 임시저장 주문서 제품 항목 Entity
 */
@Entity
@Table(
    name = "order_draft_items",
    indexes = [
        Index(name = "idx_order_draft_items_draft_id", columnList = "order_draft_id")
    ]
)
class OrderDraftItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_draft_id", nullable = false)
    val orderDraft: OrderDraft,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "box_quantity", nullable = false)
    val boxQuantity: Int = 0,

    @Column(name = "piece_quantity", nullable = false)
    val pieceQuantity: Int = 0,

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

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
