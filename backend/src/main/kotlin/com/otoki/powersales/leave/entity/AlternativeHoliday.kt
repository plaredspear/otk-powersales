package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "alternative_holiday")
@SFObject("DKRetail__AlternativeHoliday__c")
class AlternativeHoliday(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alternative_holiday_id")
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

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

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,
) : BaseEntity() {
    fun approve(confirmDate: LocalDate, changeReason: String?) {
        this.confirmAltHolidayDate = confirmDate
        this.status = "승인"
        if (changeReason != null) {
            this.changeReason = changeReason
        }

    }

    fun reject(reason: String) {
        this.status = "반려"
        this.changeReason = reason

    }

    fun canTransition(): Boolean {
        return status == "신규" || status == "조정"
    }

    companion object {
        val VALID_STATUSES = listOf("신규", "승인", "반려", "조정")
    }
}
