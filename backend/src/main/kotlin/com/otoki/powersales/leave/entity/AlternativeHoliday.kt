package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.leave.entity.converter.AltHolidayStatusConverter
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "alternative_holiday")
@SFObject("DKRetail__AlternativeHoliday__c")
@HCTable("dkretail__alternativeholiday__c")
class AlternativeHoliday(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alternative_holiday_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("DKRetail__EmployeeId__c")
    @HCColumn("dkretail__employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__ActualWorkDate__c")
    @HCColumn("dkretail__actualworkdate__c")
    @Column(name = "actual_work_date", nullable = false)
    val actualWorkDate: LocalDate,

    @SFField("DKRetail__TargetAltHolidayDate__c")
    @HCColumn("dkretail__targetaltholidaydate__c")
    @Column(name = "target_alt_holiday_date", nullable = false)
    val targetAltHolidayDate: LocalDate,

    @SFField("DKRetail__ConfirmAltHolidayDate__c")
    @HCColumn("dkretail__confirmaltholidaydate__c")
    @Column(name = "confirm_alt_holiday_date")
    var confirmAltHolidayDate: LocalDate? = null,

    @SFField("DKRetail__Status__c")
    @HCColumn("dkretail__status__c")
    @Convert(converter = AltHolidayStatusConverter::class)
    @Column(name = "status", nullable = false, length = 255)
    var status: AltHolidayStatus = AltHolidayStatus.NEW,

    @SFField("DKRetail__ChangeReason__c")
    @HCColumn("dkretail__changereason__c")
    @Column(name = "change_reason", length = 500)
    var changeReason: String? = null,

    @Column(name = "created_by", nullable = false, length = 20)
    val createdBy: String,

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

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdByEmployee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

    // -- Spec #747 카테고리 A Q2 — history 보존 의미의 사원명 캐시 --
    @SFField("DKRetail__EmpName__c")
    @HCColumn("dkretail__empname__c")
    @Column(name = "emp_name", length = 1300)
    var empName: String? = null,
) : BaseEntity() {
    fun approve(confirmDate: LocalDate, changeReason: String?) {
        this.confirmAltHolidayDate = confirmDate
        this.status = AltHolidayStatus.APPROVED
        if (changeReason != null) {
            this.changeReason = changeReason
        }

    }

    fun reject(reason: String) {
        this.status = AltHolidayStatus.REJECTED
        this.changeReason = reason

    }

    fun canTransition(): Boolean {
        return status == AltHolidayStatus.NEW || status == AltHolidayStatus.ADJUSTED
    }
}
