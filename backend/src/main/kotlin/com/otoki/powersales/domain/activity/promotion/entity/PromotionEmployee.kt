package com.otoki.powersales.domain.activity.promotion.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory1Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory2Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory3Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingTypeConverter
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("행사사원")
@Entity
@Table(name = "promotion_employee")
@SFObject("DKRetail__PromotionEmployee__c")
class PromotionEmployee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("행사사원ID")
    @Column(name = "promotion_employee_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @FieldName("행사ID")
    @Column(name = "promotion_id")
    val promotionId: Long? = null,

    @SFField("DKRetail__PromotionId__c")
    @Column(name = "promotion_sfid", length = 18)
    var promotionSfid: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    var employeeSfid: String? = null,

    @SFField("DKRetail__ScheduleDate__c")
    @FieldName("투입일")
    @Column(name = "schedule_date")
    var scheduleDate: LocalDate? = null,

    @SFField("DKRetail__WorkStatus__c")
    @FieldName("근무형태")
    @Column(name = "work_status", length = 255)
    @Convert(converter = WorkingTypeConverter::class)
    var workStatus: WorkingType? = null,

    @SFField("DKRetail__WorkType1__c")
    @FieldName("근무유형1")
    @Column(name = "work_type1", length = 255)
    @Convert(converter = WorkingCategory1Converter::class)
    var workType1: WorkingCategory1? = null,

    @SFField("DKRetail__WorkType3__c")
    @FieldName("근무유형3")
    @Column(name = "work_type3", length = 255)
    @Convert(converter = WorkingCategory3Converter::class)
    var workType3: WorkingCategory3? = null,

    @FieldName("여사원일정ID")
    @Column(name = "team_member_schedule_id")
    var teamMemberScheduleId: Long? = null,

    @SFField("DKRetail__ScheduleId__c")
    @Column(name = "team_member_schedule_sfid", length = 18)
    var teamMemberScheduleSfid: String? = null,

    @SFField("PromoCloseByTm__c")
    @FieldName("여사원마감")
    @Column(name = "promo_close_by_tm", nullable = false)
    var promoCloseByTm: Boolean = false,

    @SFField("DKRetail__BasePrice__c")
    @FieldName("기준단가")
    @Column(name = "base_price")
    var basePrice: BigDecimal? = null,

    @SFField("DKRetail__DailyTargetCount__c")
    @FieldName("일별목표갯수")
    @Column(name = "daily_target_count")
    var dailyTargetCount: BigDecimal? = null,

    @FieldName("목표금액")
    @Column(name = "target_amount")
    var targetAmount: Long? = 0,

    @FieldName("실적금액")
    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @SFField("PrimaryProductAmount__c")
    @FieldName("대표품목 매출")
    @Column(name = "primary_product_amount")
    var primaryProductAmount: BigDecimal? = null,

    @SFField("DKRetail__PrimarySalesQuantity__c")
    @FieldName("대표품목판매수량")
    @Column(name = "primary_sales_quantity")
    var primarySalesQuantity: BigDecimal? = null,

    @SFField("DKRetail__PrimarySalesPrice__c")
    @FieldName("대표품목판매단가")
    @Column(name = "primary_sales_price")
    var primarySalesPrice: BigDecimal? = null,

    @SFField("DKRetail__OtherSalesAmount__c")
    @FieldName("기타매출금금액")
    @Column(name = "other_sales_amount")
    var otherSalesAmount: BigDecimal? = null,

    @SFField("DKRetail__OtherSalesQuantity__c")
    @FieldName("기타판매수량")
    @Column(name = "other_sales_quantity")
    var otherSalesQuantity: BigDecimal? = null,

    @SFField("S3ImageUniqueKey__c")
    @FieldName("S3ImageUniqueKey")
    @Column(name = "s3_image_unique_key", length = 255)
    var s3ImageUniqueKey: String? = null,

    @SFField("Description__c")
    @FieldName("행사대체제품")
    @Column(name = "description", length = 50)
    var description: String? = null,

    @SFField("DKRetail__WorkType2__c")
    @FieldName("근무유형2")
    @Column(name = "dk_work_type2", length = 255)
    @Convert(converter = WorkingCategory2Converter::class)
    var dkWorkType2: WorkingCategory2? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", insertable = false, updatable = false)
    var promotion: Promotion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    var employee: Employee? = null

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

    // -- Spec #746 R-2 (DKRetail__ScheduleId__c FK 신설, 기존 team_member_schedule_id 컬럼 재사용) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_schedule_id", insertable = false, updatable = false)
    var teamMemberSchedule: TeamMemberSchedule? = null

    // SF Formula `DKRetail__DailyActualSalesAmount__c` 원본 공식 재현 (sf-meta-diff Q8 — 의미 오류 포함 그대로 보존, SF UI/리포트 동등성 목적).
    // 공식: (DKRetail__PrimarySalesPrice__c * DKRetail__PrimarySalesQuantity__c) + (DKRetail__OtherSalesQuantity__c * DKRetail__OtherSalesQuantity__c)
    val dkDailyActualSalesAmount: BigDecimal?
        get() {
            val price = primarySalesPrice
            val primaryQty = primarySalesQuantity
            val otherQty = otherSalesQuantity
            if (price == null && primaryQty == null && otherQty == null) return null
            val primaryTerm = (price ?: BigDecimal.ZERO) * (primaryQty ?: BigDecimal.ZERO)
            val otherTerm = (otherQty ?: BigDecimal.ZERO) * (otherQty ?: BigDecimal.ZERO)
            return primaryTerm + otherTerm
        }

    // SF Formula `DKRetail__DailyTargetAmount__c` (label=목표금액) 재현. calculated 필드라 미적재.
    // 공식: ( DKRetail__DailyTargetCount__c * DKRetail__BasePrice__c )
    // 레거시 Heroku 행사 상세(`PromotionSearch` sub-item `DailyTargetAmount`) 조원별 "목표" 동등.
    val dkDailyTargetAmount: BigDecimal?
        get() {
            val count = dailyTargetCount
            val price = basePrice
            if (count == null && price == null) return null
            return (count ?: BigDecimal.ZERO) * (price ?: BigDecimal.ZERO)
        }

    // SF Formula `DailyActualSalesAmount__c` (label=총 실적) 재현. calculated 필드라 미적재.
    // 공식: PrimaryProductAmount__c + DKRetail__OtherSalesAmount__c
    // 레거시 Heroku 행사 상세(`PromotionSearch` sub-item `DailyActualSalesAmount`) 조원별 "실적" 동등
    // — DKRetail__DailyActualSalesAmount__c(의미오류 공식) 가 아닌 이 "총 실적" 필드를 사용함에 유의.
    val dailyTotalActualSalesAmount: BigDecimal?
        get() {
            val primary = primaryProductAmount
            val other = otherSalesAmount
            if (primary == null && other == null) return null
            return (primary ?: BigDecimal.ZERO) + (other ?: BigDecimal.ZERO)
        }

    fun update(
        employeeId: Long?,
        scheduleDate: LocalDate?,
        workStatus: WorkingType?,
        workType1: WorkingCategory1?,
        workType3: WorkingCategory3?,
        basePrice: BigDecimal?,
        dailyTargetCount: BigDecimal?,
        targetAmount: Long?,
        actualAmount: Long?,
        primaryProductAmount: BigDecimal? = null,
        primarySalesQuantity: BigDecimal? = null,
        primarySalesPrice: BigDecimal? = null,
        otherSalesAmount: BigDecimal? = null,
        otherSalesQuantity: BigDecimal? = null,
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
