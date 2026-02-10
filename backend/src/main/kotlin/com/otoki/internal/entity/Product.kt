package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 제품 Entity
 * Orora 영업 시스템에서 동기화되는 제품 마스터 데이터
 */
@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_product_id", columnList = "product_id", unique = true),
        Index(name = "idx_products_product_code", columnList = "product_code"),
        Index(name = "idx_products_barcode", columnList = "barcode"),
        Index(name = "idx_products_product_name", columnList = "product_name")
    ]
)
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id", nullable = false, unique = true, length = 20)
    val productId: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "barcode", nullable = false, length = 20)
    val barcode: String,

    @Column(name = "storage_type", nullable = false, length = 10)
    val storageType: String,

    @Column(name = "shelf_life", length = 20)
    val shelfLife: String? = null,

    @Column(name = "category_mid", length = 50)
    val categoryMid: String? = null,

    @Column(name = "category_sub", length = 50)
    val categorySub: String? = null,

    @Column(name = "pieces_per_box", nullable = false)
    val piecesPerBox: Int = 1,

    @Column(name = "min_order_unit", nullable = false)
    val minOrderUnit: Int = 1,

    @Column(name = "supply_quantity", nullable = false)
    val supplyQuantity: Int = 0,

    @Column(name = "dc_quantity", nullable = false)
    val dcQuantity: Int = 0,

    @Column(name = "unit_price", nullable = false)
    val unitPrice: Long = 0,

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
