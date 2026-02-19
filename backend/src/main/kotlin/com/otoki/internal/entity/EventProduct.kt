/*
package com.otoki.internal.entity

import jakarta.persistence.*

/ **
 * 행사 제품 Entity
 * 행사에 포함된 제품 목록 (대표 제품 + 기타 제품)
 * /
@Entity
@Table(
    name = "event_products",
    indexes = [
        Index(name = "idx_event_products_event_id", columnList = "event_id"),
        Index(name = "idx_event_products_product_code", columnList = "product_code")
    ]
)
class EventProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_id", nullable = false, length = 50)
    val eventId: String,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "is_main_product", nullable = false)
    val isMainProduct: Boolean = false
)
*/
