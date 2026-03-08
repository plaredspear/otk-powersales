package com.otoki.internal.promotion.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "promotion")
class Promotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "promotion_number", nullable = false, unique = true, length = 20)
    val promotionNumber: String,

    @Column(name = "promotion_name", nullable = false, length = 200)
    var promotionName: String,

    @Column(name = "promotion_type", length = 50)
    var promotionType: String? = null,

    @Column(name = "account_id", nullable = false)
    var accountId: Int,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "primary_product_id")
    var primaryProductId: Long? = null,

    @Column(name = "other_product", length = 500)
    var otherProduct: String? = null,

    @Column(name = "message", length = 1000)
    var message: String? = null,

    @Column(name = "stand_location", length = 200)
    var standLocation: String? = null,

    @Column(name = "target_amount")
    var targetAmount: Long? = null,

    @Column(name = "cost_center_code", length = 10)
    val costCenterCode: String? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(
        promotionName: String,
        promotionType: String?,
        accountId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: String?,
        targetAmount: Long?
    ) {
        this.promotionName = promotionName
        this.promotionType = promotionType
        this.accountId = accountId
        this.startDate = startDate
        this.endDate = endDate
        this.primaryProductId = primaryProductId
        this.otherProduct = otherProduct
        this.message = message
        this.standLocation = standLocation
        this.targetAmount = targetAmount
        this.updatedAt = LocalDateTime.now()
    }

    fun softDelete() {
        this.isDeleted = true
        this.updatedAt = LocalDateTime.now()
    }
}
