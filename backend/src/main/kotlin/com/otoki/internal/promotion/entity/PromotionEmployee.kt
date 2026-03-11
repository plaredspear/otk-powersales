package com.otoki.internal.promotion.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "dkretail__promotion_employee__c")
class PromotionEmployee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "promotion_id", nullable = false)
    val promotionId: Long,

    @Column(name = "employee_sfid", nullable = false, length = 18)
    var employeeSfid: String,

    @Column(name = "schedule_date", nullable = false)
    var scheduleDate: LocalDate,

    @Column(name = "work_status", nullable = false, length = 20)
    var workStatus: String,

    @Column(name = "work_type1", nullable = false, length = 100)
    var workType1: String,

    @Column(name = "work_type3", nullable = false, length = 100)
    var workType3: String,

    @Column(name = "work_type4", length = 100)
    var workType4: String? = null,

    @Column(name = "professional_promotion_team", length = 100)
    var professionalPromotionTeam: String? = null,

    @Column(name = "schedule_id")
    var scheduleId: Long? = null,

    @Column(name = "promo_close_by_tm", nullable = false)
    var promoCloseByTm: Boolean = false,

    @Column(name = "base_price")
    var basePrice: Long? = null,

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
