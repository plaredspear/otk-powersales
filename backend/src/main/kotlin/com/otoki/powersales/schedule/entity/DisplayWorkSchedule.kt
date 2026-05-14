package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.schedule.entity.converter.SecondWorkTypeConverter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork1Converter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork3Converter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork5Converter
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 거래처 일정 Entity (진열마스터 확정 스케줄)
 * V1 스키마: displayworkschedulemaster__c
 *
 * sf-meta-diff 후속:
 * - OwnerId (`referenceTo = [Group, User]` polymorphic) 는 `owner_sfid` sync buffer +
 *   `owner_user_id` (User FK) + `owner_group_id` (Group FK) + XOR CHECK 제약
 *   `chk_display_work_schedule_owner_xor`.
 * - audit (CreatedById / LastModifiedById, `referenceTo = [User]`) FK 는 `User` 참조.
 * - ConfirmationAlert__c 는 SF Formula (`calculated=true`) — DB 컬럼 부재.
 * - LastMonthRevenue__c 는 SF `double precision=18 scale=0` — `BigDecimal` 매핑.
 */
@Entity
@Table(name = "display_work_schedule")
@SFObject("DisplayWorkScheduleMaster__c")
@HCTable("displayworkschedulemaster__c")
class DisplayWorkSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "display_work_schedule_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Account__c")
    @HCColumn("account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @HCColumn("fullname__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed")
    var confirmed: Boolean? = null,

    @SFField("TypeOfWork1__c")
    @HCColumn("typeofwork1__c")
    @Column(name = "type_of_work1", length = 255)
    @Convert(converter = TypeOfWork1Converter::class)
    val typeOfWork1: TypeOfWork1? = null,

    @SFField("TypeOfWork3__c")
    @HCColumn("typeofwork3__c")
    @Column(name = "type_of_work3", length = 255)
    @Convert(converter = TypeOfWork3Converter::class)
    val typeOfWork3: TypeOfWork3? = null,

    @SFField("TypeOfWork4__c")
    @HCColumn("typeofwork4__c")
    @Column(name = "type_of_work4", length = 255)
    @Convert(converter = SecondWorkTypeConverter::class)
    val typeOfWork4: SecondWorkType? = null,

    @SFField("TypeOfWork5__c")
    @HCColumn("typeofwork5__c")
    @Column(name = "type_of_work5", length = 255)
    @Convert(converter = TypeOfWork5Converter::class)
    val typeOfWork5: TypeOfWork5? = null,

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    val ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 20)
    val costCenterCode: String? = null,

    @SFField("LastMonthRevenue__c")
    @HCColumn("lastmonthrevenue__c")
    @Column(name = "last_month_revenue", precision = 18, scale = 0)
    val lastMonthRevenue: BigDecimal? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    // OwnerId polymorphic R-2 (referenceTo = [Group, User]) — sfid prefix `005` = User / `00G` = Group.
    // XOR CHECK 제약 chk_display_work_schedule_owner_xor.

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
