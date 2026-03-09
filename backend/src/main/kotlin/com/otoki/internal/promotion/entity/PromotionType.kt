package com.otoki.internal.promotion.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "dkretail__promotion_type")
class PromotionType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, unique = true, length = 50)
    var name: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(name: String, displayOrder: Int) {
        this.name = name
        this.displayOrder = displayOrder
        this.updatedAt = LocalDateTime.now()
    }

    fun deactivate() {
        this.isActive = false
        this.updatedAt = LocalDateTime.now()
    }
}
