package com.otoki.internal.promotion.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "promotion")
@SFObject("DKRetail__Promotion__c")
class Promotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    val id: Long = 0,

    @SFField("Name")
    @Column(name = "promotion_number", nullable = false, unique = true, length = 20)
    val promotionNumber: String,

    @SFField("DKRetail__PromotionName__c")
    @Column(name = "promotion_name", nullable = true, length = 200)
    var promotionName: String? = null,

    @SFField("DKRetail__PromotionType__c")
    @Column(name = "promotion_type_id")
    var promotionTypeId: Long? = null,

    @SFField("AccId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("DKRetail__StartDate__c")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @SFField("DKRetail__EndDate__c")
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "primary_product_id")
    var primaryProductId: Long? = null,

    @SFField("DKRetail__PrimaryProductId__c")
    @Column(name = "primary_product_sfid", length = 18)
    var primaryProductSfid: String? = null,

    @SFField("DKRetail__OtherProduct__c")
    @Column(name = "other_product", length = 200)
    var otherProduct: String? = null,

    @SFField("DKRetail__Message__c")
    @Column(name = "message", length = 255)
    var message: String? = null,

    @SFField("DKRetail__StandLocation__c")
    @Column(name = "stand_location", length = 200)
    var standLocation: String? = null,

    @SFField("DKRetail__TargetAmount__c")
    @Column(name = "target_amount")
    var targetAmount: Long? = null,

    @SFField("DKRetail__ActualAmount__c")
    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @SFField("DKRetail__Remark__c")
    @Column(name = "remark", length = 200)
    var remark: String? = null,

    @SFField("BranchName__c")
    @Column(name = "branch_name", length = 100)
    var branchName: String? = null,

    @SFField("Category1__c")
    @Column(name = "category", length = 50)
    var category: String? = null,

    @SFField("DKRetail__ProductType__c")
    @Column(name = "product_type", length = 50)
    var productType: String? = null,

    @SFField("DKRetail__AccId__c")
    @Column(name = "deprecated_acc_sfid", length = 18)
    var deprecatedAccSfid: String? = null,

    @SFField("AccCode__c")
    @Column(name = "account_code", length = 100)
    var accountCode: String? = null,

    @SFField("ActualAmount__c")
    @Column(name = "actual_amount_won")
    var actualAmountWon: Long? = null,

    @SFField("ProductCode__c")
    @Column(name = "product_code", length = 100)
    var productCode: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_type_id", insertable = false, updatable = false)
    var promotionType: PromotionType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_product_id", insertable = false, updatable = false)
    var primaryProduct: Product? = null

    fun update(
        promotionName: String?,
        promotionTypeId: Long?,
        account: Account,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: String?,
        category: String?,
        productType: String?,
        branchName: String?,
        remark: String?
    ) {
        this.promotionName = promotionName
        this.promotionTypeId = promotionTypeId
        this.account = account
        this.startDate = startDate
        this.endDate = endDate
        this.primaryProductId = primaryProductId
        this.otherProduct = otherProduct
        this.message = message
        this.standLocation = standLocation
        this.category = category
        this.productType = productType
        this.branchName = branchName
        this.remark = remark

    }

    fun updateAmounts(targetAmount: Long, actualAmount: Long) {
        this.targetAmount = targetAmount
        this.actualAmount = actualAmount

    }

    fun softDelete() {
        this.isDeleted = true

    }
}
