package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.promotion.entity.converter.ProductTemperatureTypeConverter
import com.otoki.powersales.promotion.entity.converter.StandLocationConverter
import com.otoki.powersales.promotion.enums.ProductTemperatureType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "promotion")
@SFObject("DKRetail__Promotion__c")
@HCTable("dkretail__promotion__c")
class Promotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "promotion_number", nullable = false, unique = true, length = 80)
    val promotionNumber: String,


    @SFField("DKRetail__PromotionType__c")
    @HCColumn("dkretail__promotiontype__c")
    @Column(name = "promotion_type_id")
    var promotionTypeId: Long? = null,

    @SFField("AccId__c")
    @HCColumn("accid__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("DKRetail__StartDate__c")
    @HCColumn("dkretail__startdate__c")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @SFField("DKRetail__EndDate__c")
    @HCColumn("dkretail__enddate__c")
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "primary_product_id")
    var primaryProductId: Long? = null,

    @SFField("DKRetail__PrimaryProductId__c")
    @HCColumn("dkretail__primaryproductid__c")
    @Column(name = "primary_product_sfid", length = 18)
    var primaryProductSfid: String? = null,

    @SFField("DKRetail__OtherProduct__c")
    @HCColumn("dkretail__otherproduct__c")
    @Column(name = "other_product", length = 200)
    var otherProduct: String? = null,

    @SFField("DKRetail__Message__c")
    @HCColumn("dkretail__message__c")
    @Column(name = "message", length = 255)
    var message: String? = null,

    @SFField("DKRetail__StandLocation__c")
    @HCColumn("dkretail__standlocation__c")
    @Column(name = "stand_location", length = 255)
    @Convert(converter = StandLocationConverter::class)
    var standLocation: StandLocation? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @SFField("DKRetail__Remark__c")
    @HCColumn("dkretail__remark__c")
    @Column(name = "remark", length = 200)
    var remark: String? = null,

    @SFField("DKRetail__ProductType__c")
    @HCColumn("dkretail__producttype__c")
    @Column(name = "product_type", length = 255)
    @Convert(converter = ProductTemperatureTypeConverter::class)
    var productType: ProductTemperatureType? = null,

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account,

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
    @HCColumn("dkretail__actualamount__c")
    @Column(name = "dk_actual_amount")
    var dkActualAmount: Double? = null,

    @SFField("DKRetail__TargetAmount__c")
    @HCColumn("dkretail__targetamount__c")
    @Column(name = "dk_target_amount")
    var dkTargetAmount: Double? = null,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_type_id", insertable = false, updatable = false)
    var promotionType: PromotionType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_product_id", insertable = false, updatable = false)
    var primaryProduct: Product? = null

    fun update(
        promotionTypeId: Long?,
        account: Account,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: StandLocation?,
        productType: ProductTemperatureType?,
        remark: String?
    ) {
        this.promotionTypeId = promotionTypeId
        this.account = account
        this.startDate = startDate
        this.endDate = endDate
        this.primaryProductId = primaryProductId
        this.otherProduct = otherProduct
        this.message = message
        this.standLocation = standLocation
        this.productType = productType
        this.remark = remark

    }

    fun softDelete() {
        this.isDeleted = true

    }
}
