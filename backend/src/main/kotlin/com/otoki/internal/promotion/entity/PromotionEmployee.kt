package com.otoki.internal.promotion.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "promotion_employee")
@SFObject("DKRetail__PromotionEmployee__c")
class PromotionEmployee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @SFField("DKRetail__PromotionId__c")
    @Column(name = "promotion_id", nullable = false)
    val promotionId: Long,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @SFField("DKRetail__ScheduleDate__c")
    @Column(name = "schedule_date")
    var scheduleDate: LocalDate? = null,

    @SFField("DKRetail__WorkStatus__c")
    @Column(name = "work_status", length = 20)
    var workStatus: String? = null,

    @SFField("DKRetail__WorkType1__c")
    @Column(name = "work_type1", length = 100)
    var workType1: String? = null,

    @SFField("DKRetail__WorkType3__c")
    @Column(name = "work_type3", length = 100)
    var workType3: String? = null,

    @SFField("WorkType4__c")
    @Column(name = "work_type4", length = 100)
    var workType4: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Column(name = "professional_promotion_team", length = 100)
    var professionalPromotionTeam: String? = null,

    @SFField("DKRetail__ScheduleId__c")
    @Column(name = "schedule_id")
    var scheduleId: Long? = null,

    @SFField("PromoCloseByTm__c")
    @Column(name = "promo_close_by_tm", nullable = false)
    var promoCloseByTm: Boolean = false,

    @SFField("DKRetail__BasePrice__c")
    @Column(name = "base_price")
    var basePrice: Long? = null,

    @SFField("DKRetail__DailyTargetCount__c")
    @Column(name = "daily_target_count")
    var dailyTargetCount: Int? = null,

    @Column(name = "target_amount")
    var targetAmount: Long? = 0,

    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @SFField("PrimaryProductAmount__c")
    @Column(name = "primary_product_amount")
    var primaryProductAmount: Long? = null,

    @SFField("DKRetail__PrimarySalesQuantity__c")
    @Column(name = "primary_sales_quantity")
    var primarySalesQuantity: Int? = null,

    @SFField("DKRetail__PrimarySalesPrice__c")
    @Column(name = "primary_sales_price")
    var primarySalesPrice: Long? = null,

    @SFField("DKRetail__OtherSalesAmount__c")
    @Column(name = "other_sales_amount")
    var otherSalesAmount: Long? = null,

    @SFField("DKRetail__OtherSalesQuantity__c")
    @Column(name = "other_sales_quantity")
    var otherSalesQuantity: Int? = null,

    @SFField("S3ImageUniqueKey__c")
    @Column(name = "s3_image_unique_key", length = 255)
    var s3ImageUniqueKey: String? = null,

) : BaseEntity() {
    fun update(
        employeeId: Long?,
        scheduleDate: LocalDate?,
        workStatus: String?,
        workType1: String?,
        workType3: String?,
        workType4: String?,
        professionalPromotionTeam: String?,
        basePrice: Long?,
        dailyTargetCount: Int?,
        targetAmount: Long?,
        actualAmount: Long?,
        primaryProductAmount: Long? = null,
        primarySalesQuantity: Int? = null,
        primarySalesPrice: Long? = null,
        otherSalesAmount: Long? = null,
        otherSalesQuantity: Int? = null,
        s3ImageUniqueKey: String? = null
    ) {
        this.employeeId = employeeId
        this.scheduleDate = scheduleDate
        this.workStatus = workStatus
        this.workType1 = workType1
        this.workType3 = workType3
        this.workType4 = workType4
        this.professionalPromotionTeam = professionalPromotionTeam
        this.basePrice = basePrice
        this.dailyTargetCount = dailyTargetCount
        this.targetAmount = targetAmount
        this.actualAmount = actualAmount
        this.primaryProductAmount = primaryProductAmount
        this.primarySalesQuantity = primarySalesQuantity
        this.primarySalesPrice = primarySalesPrice
        this.otherSalesAmount = otherSalesAmount
        this.otherSalesQuantity = otherSalesQuantity
        this.s3ImageUniqueKey = s3ImageUniqueKey

    }
}
