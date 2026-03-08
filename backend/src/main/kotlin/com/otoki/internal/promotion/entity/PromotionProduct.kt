package com.otoki.internal.promotion.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "promotion_product")
class PromotionProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "promotion_id", nullable = false)
    val promotionId: Long,

    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "is_main_product", nullable = false)
    val isMainProduct: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
