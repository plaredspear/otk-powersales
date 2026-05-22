package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.promotion.entity.converter.ProductTemperatureTypeConverter
import com.otoki.powersales.promotion.entity.converter.PromotionTypeConverter
import com.otoki.powersales.promotion.entity.converter.StandLocationConverter
import com.otoki.powersales.promotion.enums.ProductTemperatureType
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.user.entity.User
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

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "promotion_number", nullable = false, unique = true, length = 80)
    val promotionNumber: String,


    @SFField("DKRetail__PromotionType__c")
    @Column(name = "promotion_type", length = 255)
    @Convert(converter = PromotionTypeConverter::class)
    var promotionType: PromotionType? = null,

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
    @Column(name = "stand_location", length = 255)
    @Convert(converter = StandLocationConverter::class)
    var standLocation: StandLocation? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @SFField("DKRetail__Remark__c")
    @Column(name = "remark", length = 200)
    var remark: String? = null,

    @SFField("DKRetail__ProductType__c")
    @Column(name = "product_type", length = 255)
    @Convert(converter = ProductTemperatureTypeConverter::class)
    var productType: ProductTemperatureType? = null,

    // SF Promotion.Category1__c (string, length=1300, label="제품유형")
    // 레거시 PromotionEmployeeTriggerHandler 의 대표제품 vs 전문행사조 매칭 검증 입력
    @SFField("Category1__c")
    @Column(name = "category1", length = 1300)
    var category1: String? = null,

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

    @SFField("IsDeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @SFField("DKRetail__ActualAmount__c")
    @Column(name = "dk_actual_amount")
    var dkActualAmount: Double? = null,

    @SFField("DKRetail__TargetAmount__c")
    @Column(name = "dk_target_amount")
    var dkTargetAmount: Double? = null,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_product_id", insertable = false, updatable = false)
    var primaryProduct: Product? = null

    fun update(
        promotionType: PromotionType?,
        account: Account,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: StandLocation?,
        productType: ProductTemperatureType?,
        category1: String?,
        remark: String?
    ) {
        this.promotionType = promotionType
        this.account = account
        this.startDate = startDate
        this.endDate = endDate
        this.primaryProductId = primaryProductId
        this.otherProduct = otherProduct
        this.message = message
        this.standLocation = standLocation
        this.productType = productType
        this.category1 = category1
        this.remark = remark

    }

    fun softDelete() {
        this.isDeleted = true

    }
}
