package com.otoki.powersales.domain.org.leave.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.org.leave.entity.converter.AltHolidayStatusConverter
import com.otoki.powersales.domain.org.leave.enums.AltHolidayStatus
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("대체공휴일")
@Entity
@Table(name = "alternative_holiday")
@SFObject("DKRetail__AlternativeHoliday__c")
class AlternativeHoliday(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("대체공휴일ID")
    @Column(name = "alternative_holiday_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__ActualWorkDate__c")
    @FieldName("대휴대상일자")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "actual_work_date")
    val actualWorkDate: LocalDate? = null,

    @SFField("DKRetail__TargetAltHolidayDate__c")
    @FieldName("대휴신청일자")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "target_alt_holiday_date")
    val targetAltHolidayDate: LocalDate? = null,

    @SFField("DKRetail__ConfirmAltHolidayDate__c")
    @FieldName("대휴일자")
    @Column(name = "confirm_alt_holiday_date")
    var confirmAltHolidayDate: LocalDate? = null,

    @SFField("DKRetail__Status__c")
    @Convert(converter = AltHolidayStatusConverter::class)
    @FieldName("상태")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "status", length = 255)
    var status: AltHolidayStatus? = AltHolidayStatus.NEW,

    @SFField("DKRetail__ChangeReason__c")
    @FieldName("변경사유")
    @Column(name = "change_reason", length = 255)
    var changeReason: String? = null,

    @FieldName("생성자사번")
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
    @FieldName("삭제여부")
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
