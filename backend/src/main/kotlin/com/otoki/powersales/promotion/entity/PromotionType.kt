package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "promotion_type")
class PromotionType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, unique = true, length = 50)
    var name: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseEntity() {
    fun update(name: String, displayOrder: Int) {
        this.name = name
        this.displayOrder = displayOrder

    }

    fun deactivate() {
        this.isActive = false

    }
}
