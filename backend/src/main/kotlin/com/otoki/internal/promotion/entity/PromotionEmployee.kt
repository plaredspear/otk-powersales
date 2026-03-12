package com.otoki.internal.promotion.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "dkretail__promotion_employee__c")
@SFObject("DKRetail__PromotionEmployee__c")
@HCTable("dkretail__promotionemployee__c")
class PromotionEmployee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Long = 0,

    @SFField("DKRetail__PromotionId__c")
    @HCColumn("dkretail__promotionid__c")
    @Column(name = "promotion_id", nullable = false)
    val promotionId: Long,

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
    @Column(name = "work_status", length = 20)
    var workStatus: String? = null,

    @SFField("DKRetail__WorkType1__c")
    @HCColumn("dkretail__worktype1__c")
    @Column(name = "work_type1", length = 100)
    var workType1: String? = null,

    @SFField("DKRetail__WorkType3__c")
    @HCColumn("dkretail__worktype3__c")
    @Column(name = "work_type3", length = 100)
    var workType3: String? = null,

    @SFField("WorkType4__c")
    @HCColumn("worktype4__c")
    @Column(name = "work_type4", length = 100)
    var workType4: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @HCColumn("professionalpromotionteam__c")
    @Column(name = "professional_promotion_team", length = 100)
    var professionalPromotionTeam: String? = null,

    @SFField("DKRetail__ScheduleId__c")
    @HCColumn("dkretail__scheduleid__c")
    @Column(name = "schedule_id")
    var scheduleId: Long? = null,

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
    var dailyTargetCount: Int? = null,

    @Column(name = "target_amount")
    var targetAmount: Long? = 0,

    @Column(name = "actual_amount")
    var actualAmount: Long? = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(
        employeeSfid: String,
        scheduleDate: LocalDate,
        workStatus: String,
        workType1: String,
        workType3: String,
        workType4: String?,
        professionalPromotionTeam: String?,
        basePrice: Long?,
        dailyTargetCount: Int?,
        targetAmount: Long?,
        actualAmount: Long?
    ) {
        this.employeeSfid = employeeSfid
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
        this.updatedAt = LocalDateTime.now()
    }
}
