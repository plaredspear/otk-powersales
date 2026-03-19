package com.otoki.internal.leave.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "alternative_holiday")
@SFObject("DKRetail__AlternativeHoliday__c")
class AlternativeHoliday(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_number", nullable = false, length = 20)
    val employeeNumber: String,

    @SFField("DKRetail__EmpName__c")
    @Column(name = "employee_name", nullable = false, length = 50)
    val employeeName: String,

    @SFField("DKRetail__ActualWorkDate__c")
    @Column(name = "actual_work_date", nullable = false)
    val actualWorkDate: LocalDate,

    @SFField("DKRetail__TargetAltHolidayDate__c")
    @Column(name = "target_alt_holiday_date", nullable = false)
    val targetAltHolidayDate: LocalDate,

    @SFField("DKRetail__ConfirmAltHolidayDate__c")
    @Column(name = "confirm_alt_holiday_date")
    var confirmAltHolidayDate: LocalDate? = null,

    @SFField("DKRetail__Status__c")
    @Column(name = "status", nullable = false, length = 10)
    var status: String = "신규",

    @SFField("DKRetail__ChangeReason__c")
    @Column(name = "change_reason", length = 500)
    var changeReason: String? = null,

    @Column(name = "created_by", nullable = false, length = 20)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun approve(confirmDate: LocalDate, changeReason: String?) {
        this.confirmAltHolidayDate = confirmDate
        this.status = "승인"
        if (changeReason != null) {
            this.changeReason = changeReason
        }
        this.updatedAt = LocalDateTime.now()
    }

    fun reject(reason: String) {
        this.status = "반려"
        this.changeReason = reason
        this.updatedAt = LocalDateTime.now()
    }

    fun canTransition(): Boolean {
        return status == "신규" || status == "조정"
    }

    companion object {
        val VALID_STATUSES = listOf("신규", "승인", "반려", "조정")
    }
}
