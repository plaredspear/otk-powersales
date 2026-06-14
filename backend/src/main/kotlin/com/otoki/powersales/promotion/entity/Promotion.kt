package com.otoki.powersales.promotion.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.promotion.entity.converter.PromotionTypeConverter
import com.otoki.powersales.promotion.entity.converter.StandLocationConverter
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener

@EntityListeners(OwnerUserDefaultListener::class)
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

    // -- Spec #849: deprecated SF lookup `DKRetail__AccId__c` (운영라벨 "사용안함") 부활 (raw FK id 컬럼만, @ManyToOne 미도입) --
    @SFField("DKRetail__AccId__c")
    @Column(name = "dk_account_sfid", length = 18)
    var dkAccountSfid: String? = null,

    @Column(name = "dk_account_id")
    var dkAccountId: Long? = null,

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

    // 상품유형(SF picklist `DKRetail__ProductType__c`). 행사명 formula 가
    // `TEXT(DKRetail__ProductType__c)` 로 원시 picklist 값을 그대로 통과시키므로,
    // enum 으로 제약하면 비활성 값(`냉동`/`카레` 등)이 변환 시 유실된다.
    // 레거시와 동일하게 원시 문자열로 보존한다.
    @SFField("DKRetail__ProductType__c")
    @Column(name = "product_type", length = 255)
    var productType: String? = null,

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

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
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

    // SF formula `Category1__c = DKRetail__PrimaryProductId__r.StoreCondition__c` 동등.
    // 대표제품(primaryProduct) 의 storeConditionText 를 실시간 derive (저장 컬럼 아님).
    val category1: String?
        get() = primaryProduct?.storeConditionText

    fun update(
        promotionType: PromotionType?,
        account: Account,
        startDate: LocalDate,
        endDate: LocalDate,
        primaryProductId: Long?,
        otherProduct: String?,
        message: String?,
        standLocation: StandLocation?,
        productType: String?,
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
        this.remark = remark

    }

    fun softDelete() {
        this.isDeleted = true

    }
}
