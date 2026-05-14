package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.common.entity.converter.WorkingCategory1Converter
import com.otoki.powersales.common.entity.converter.WorkingCategory2Converter
import com.otoki.powersales.common.entity.converter.WorkingCategory3Converter
import com.otoki.powersales.common.entity.converter.WorkingTypeConverter
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "promotion_employee")
@SFObject("DKRetail__PromotionEmployee__c")
@HCTable("dkretail__promotionemployee__c")
class PromotionEmployee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_employee_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @Column(name = "promotion_id", nullable = false)
    val promotionId: Long,

    @SFField("DKRetail__PromotionId__c")
    @HCColumn("dkretail__promotionid__c")
    @Column(name = "promotion_sfid", length = 18)
    var promotionSfid: String? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @SFField("DKRetail__EmployeeId__c")
    @HCColumn("dkretail__employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    var employeeSfid: String? = null,

    @SFField("DKRetail__ScheduleDate__c")
    @HCColumn("dkretail__scheduledate__c")
    @Column(name = "schedule_date")
    var scheduleDate: LocalDate? = null,

    @SFField("DKRetail__WorkStatus__c")
    @HCColumn("dkretail__workstatus__c")
    @Column(name = "work_status", length = 255)
    @Convert(converter = WorkingTypeConverter::class)
    var workStatus: WorkingType? = null,

    @SFField("DKRetail__WorkType1__c")
    @HCColumn("dkretail__worktype1__c")
    @Column(name = "work_type1", length = 255)
    @Convert(converter = WorkingCategory1Converter::class)
    var workType1: WorkingCategory1? = null,

    @SFField("DKRetail__WorkType3__c")
    @HCColumn("dkretail__worktype3__c")
    @Column(name = "work_type3", length = 255)
    @Convert(converter = WorkingCategory3Converter::class)
    var workType3: WorkingCategory3? = null,

    @Column(name = "team_member_schedule_id")
    var teamMemberScheduleId: Long? = null,

    @SFField("DKRetail__ScheduleId__c")
    @HCColumn("dkretail__scheduleid__c")
    @Column(name = "team_member_schedule_sfid", length = 18)
    var teamMemberScheduleSfid: String? = null,

    @SFField("PromoCloseByTm__c")
    @HCColumn("promoclosebytm__c")
    @Column(name = "promo_close_by_tm", nullable = false)
    var promoCloseByTm: Boolean = false,

    @SFField("DKRetail__BasePrice__c")
    @HCColumn("dkretail__baseprice__c")
    @Column(name = "base_price")
    var basePrice: Long? = null,

    @SFField("DKRetail__DailyTargetCount__c")
    @HCColumn("dkretail__dailytargetcount__c")
    @Column(name = "daily_target_count")
    var dailyTargetCount: Long? = null,

    @Column(name = "target_amount")
    var targetAmount: Long? = 0,

    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @SFField("PrimaryProductAmount__c")
    @HCColumn("primaryproductamount__c")
    @Column(name = "primary_product_amount")
    var primaryProductAmount: Long? = null,

    @SFField("DKRetail__PrimarySalesQuantity__c")
    @HCColumn("dkretail__primarysalesquantity__c")
    @Column(name = "primary_sales_quantity")
    var primarySalesQuantity: Long? = null,

    @SFField("DKRetail__PrimarySalesPrice__c")
    @HCColumn("dkretail__primarysalesprice__c")
    @Column(name = "primary_sales_price")
    var primarySalesPrice: Long? = null,

    @SFField("DKRetail__OtherSalesAmount__c")
    @HCColumn("dkretail__othersalesamount__c")
    @Column(name = "other_sales_amount")
    var otherSalesAmount: Long? = null,

    @SFField("DKRetail__OtherSalesQuantity__c")
    @HCColumn("dkretail__othersalesquantity__c")
    @Column(name = "other_sales_quantity")
    var otherSalesQuantity: Long? = null,

    @SFField("S3ImageUniqueKey__c")
    @HCColumn("s3imageuniquekey__c")
    @Column(name = "s3_image_unique_key", length = 255)
    var s3ImageUniqueKey: String? = null,

    @SFField("Description__c")
    @HCColumn("description__c")
    @Column(name = "description", length = 50)
    var description: String? = null,

    @SFField("DKRetail__WorkType2__c")
    @HCColumn("dkretail__worktype2__c")
    @Column(name = "dk_work_type2", length = 255)
    @Convert(converter = WorkingCategory2Converter::class)
    var dkWorkType2: WorkingCategory2? = null,

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

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", insertable = false, updatable = false)
    var promotion: Promotion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    var employee: Employee? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

    // -- Spec #746 R-2 (DKRetail__ScheduleId__c FK 신설, 기존 team_member_schedule_id 컬럼 재사용) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_schedule_id", insertable = false, updatable = false)
    var teamMemberSchedule: com.otoki.powersales.schedule.entity.TeamMemberSchedule? = null

    // SF Formula `DKRetail__DailyActualSalesAmount__c` 원본 공식 재현 (sf-meta-diff Q8 — 의미 오류 포함 그대로 보존, SF UI/리포트 동등성 목적).
    // 공식: (DKRetail__PrimarySalesPrice__c * DKRetail__PrimarySalesQuantity__c) + (DKRetail__OtherSalesQuantity__c * DKRetail__OtherSalesQuantity__c)
    val dkDailyActualSalesAmount: Long?
        get() {
            val price = primarySalesPrice
            val primaryQty = primarySalesQuantity
            val otherQty = otherSalesQuantity
            if (price == null && primaryQty == null && otherQty == null) return null
            val primaryTerm = (price ?: 0) * (primaryQty ?: 0)
            val otherTerm = (otherQty ?: 0) * (otherQty ?: 0)
            return primaryTerm + otherTerm
        }

    fun update(
        employeeId: Long?,
        scheduleDate: LocalDate?,
        workStatus: WorkingType?,
        workType1: WorkingCategory1?,
        workType3: WorkingCategory3?,
        basePrice: Long?,
        dailyTargetCount: Long?,
        targetAmount: Long?,
        actualAmount: Long?,
        primaryProductAmount: Long? = null,
        primarySalesQuantity: Long? = null,
        primarySalesPrice: Long? = null,
        otherSalesAmount: Long? = null,
        otherSalesQuantity: Long? = null,
        s3ImageUniqueKey: String? = null
    ) {
        this.employeeId = employeeId
        this.scheduleDate = scheduleDate
        this.workStatus = workStatus
        this.workType1 = workType1
        this.workType3 = workType3
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
