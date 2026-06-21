package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.domain.activity.schedule.entity.converter.SecondWorkTypeConverter
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 출근현황 Entity
 * Salesforce: DKRetail__CommuteLog__c
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("출근현황")
@Entity
@Table(name = "attendance_log")
@SFObject("DKRetail__CommuteLog__c")
class AttendanceLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("출근현황ID")
    @Column(name = "attendance_log_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__CommuteDate__c")
    @FieldName("출근일시")
    @Column(name = "attendance_date")
    val attendanceDate: LocalDateTime? = null,

    @SFField("DKRetail__AccId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    val accountId: Long? = null,

    @SFField("DKRetail__SecondWorkType__c")
    @FieldName("2차근무형태")
    @Column(name = "second_work_type", length = 255)
    @Convert(converter = SecondWorkTypeConverter::class)
    val secondWorkType: SecondWorkType? = null,

    @SFField("DKRetail__Reason__c")
    @FieldName("사유")
    @Column(name = "reason", length = 255)
    val reason: String? = null,

    /**
     * 출근 종류 (Spec #587).
     * REGULAR: 일반 / DISPLAY: 진열 마스터 연계.
     * DB 체크 제약 없음 — 본 enum 으로만 검증 (backend-conventions.md "Enum 컬럼 정책").
     */
    @Enumerated(EnumType.STRING)
    @FieldName("출근종류")
    @Column(name = "attendance_type", length = 20)
    val attendanceType: AttendanceType? = AttendanceType.REGULAR,

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    val account: Account? = null,

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

    ) : BaseEntity()
