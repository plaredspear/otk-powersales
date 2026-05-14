package com.otoki.powersales.schedule.entity

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.schedule.entity.converter.SecondWorkTypeConverter
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 출근현황 Entity
 * Salesforce: DKRetail__CommuteLog__c
 */
@Entity
@Table(name = "attendance_log")
@SFObject("DKRetail__CommuteLog__c")
@HCTable("dkretail__commutelog__c")
class AttendanceLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_log_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @HCColumn("dkretail__employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__CommuteDate__c")
    @HCColumn("dkretail__commutedate__c")
    @Column(name = "attendance_date")
    val attendanceDate: LocalDateTime? = null,

    @SFField("DKRetail__AccId__c")
    @HCColumn("dkretail__accid__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @Column(name = "account_id")
    val accountId: Int? = null,

    @SFField("DKRetail__SecondWorkType__c")
    @HCColumn("dkretail__secondworktype__c")
    @Column(name = "second_work_type", length = 255)
    @Convert(converter = SecondWorkTypeConverter::class)
    val secondWorkType: SecondWorkType? = null,

    @SFField("DKRetail__Reason__c")
    @HCColumn("dkretail__reason__c")
    @Column(name = "reason", length = 255)
    val reason: String? = null,

    /**
     * 출근 종류 (Spec #587).
     * REGULAR: 일반 / DISPLAY: 진열 마스터 연계.
     * DB 체크 제약 없음 — 본 enum 으로만 검증 (backend-conventions.md "Enum 컬럼 정책").
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 20)
    val attendanceType: AttendanceType = AttendanceType.REGULAR,

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
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    val account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

) : BaseEntity()
