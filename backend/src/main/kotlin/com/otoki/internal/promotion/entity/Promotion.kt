package com.otoki.internal.promotion.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "dkretail__promotion__c")
class Promotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "promotion_number", nullable = false, unique = true, length = 20)
    val promotionNumber: String,

    @Column(name = "promotion_name", nullable = true, length = 200)
    var promotionName: String? = null,

    @Column(name = "promotion_type_id")
    var promotionTypeId: Long? = null,

    @Column(name = "account_id", nullable = false)
    var accountId: Int,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "primary_product_id")
    var primaryProductId: Long? = null,

    @Column(name = "other_product", length = 200)
    var otherProduct: String? = null,

    @Column(name = "message", length = 255)
    var message: String? = null,

    @Column(name = "stand_location", length = 200)
    var standLocation: String? = null,

    @Column(name = "target_amount")
    var targetAmount: Long? = null,

    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @Column(name = "remark", length = 200)
    var remark: String? = null,

    @Column(name = "branch_name", length = 100)
    var branchName: String? = null,

    @Column(name = "category", length = 50)
    var category: String? = null,

    @Column(name = "product_type", length = 50)
    var productType: String? = null,

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @Column(name = "professional_team", length = 100)
    var professionalTeam: String? = null,

    @Column(name = "external_id", length = 50)
    var externalId: String? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(
        promotionName: String?,
        promotionTypeId: Long?,
        accountId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: String?,
        targetAmount: Long?,
        category: String?,
        productType: String?,
        branchName: String?,
        professionalTeam: String?,
        externalId: String?,
        remark: String?
    ) {
        this.promotionName = promotionName
        this.promotionTypeId = promotionTypeId
        this.accountId = accountId
        this.startDate = startDate
        this.endDate = endDate
        this.primaryProductId = primaryProductId
        this.otherProduct = otherProduct
        this.message = message
        this.standLocation = standLocation
        this.targetAmount = targetAmount
        this.category = category
        this.productType = productType
        this.branchName = branchName
        this.professionalTeam = professionalTeam
        this.externalId = externalId
        this.remark = remark
        this.updatedAt = LocalDateTime.now()
    }

    fun softDelete() {
        this.isDeleted = true
        this.updatedAt = LocalDateTime.now()
    }
}
