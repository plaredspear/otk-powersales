package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 즐겨찾기 제품 Entity
 * 사용자가 즐겨찾기한 제품 정보를 관리한다.
 * 사용자당 동일 제품 중복 방지를 위해 Unique Constraint를 적용한다.
 */
@Entity
@Table(
    name = "favorite_products",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_favorite_products_user_product",
            columnNames = ["user_id", "product_id"]
        )
    ],
    indexes = [
        Index(name = "idx_favorite_products_user_id", columnList = "user_id")
    ]
)
class FavoriteProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
