package com.otoki.powersales.leave.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.leave.entity.converter.AltHolidayStatusConverter
import com.otoki.powersales.leave.enums.AltHolidayStatus
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener

@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "alternative_holiday")
@SFObject("DKRetail__AlternativeHoliday__c")
class AlternativeHoliday(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alternative_holiday_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

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
    @Convert(converter = AltHolidayStatusConverter::class)
    @Column(name = "status", nullable = false, length = 255)
    var status: AltHolidayStatus = AltHolidayStatus.NEW,

    @SFField("DKRetail__ChangeReason__c")
    @Column(name = "change_reason", length = 255)
    var changeReason: String? = null,

    @Column(name = "created_by_emp_no", length = 20)
    val createdByEmpNo: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    // OwnerId polymorphic [Group, User] — XOR (둘 중 하나만 NOT NULL)
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
