package com.otoki.powersales.schedule.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.schedule.entity.converter.SecondWorkTypeConverter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork1Converter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork3Converter
import com.otoki.powersales.schedule.entity.converter.TypeOfWork5Converter
import com.otoki.powersales.schedule.enums.SecondWorkType
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener

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
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "display_work_schedule")
@SFObject("DisplayWorkScheduleMaster__c")
class DisplayWorkSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "display_work_schedule_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("StartDate__c")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @Column(name = "confirmed")
    var confirmed: Boolean? = null,

    @SFField("TypeOfWork1__c")
    @Column(name = "type_of_work1", length = 255)
    @Convert(converter = TypeOfWork1Converter::class)
    var typeOfWork1: TypeOfWork1? = null,

    @SFField("TypeOfWork3__c")
    @Column(name = "type_of_work3", length = 255)
    @Convert(converter = TypeOfWork3Converter::class)
    var typeOfWork3: TypeOfWork3? = null,

    @SFField("TypeOfWork4__c")
    @Column(name = "type_of_work4", length = 255)
    @Convert(converter = SecondWorkTypeConverter::class)
    var typeOfWork4: SecondWorkType? = null,

    @SFField("TypeOfWork5__c")
    @Column(name = "type_of_work5", length = 255)
    @Convert(converter = TypeOfWork5Converter::class)
    var typeOfWork5: TypeOfWork5? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    val ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 20)
    var costCenterCode: String? = null,

    @SFField("LastMonthRevenue__c")
    @Column(name = "last_month_revenue", precision = 18, scale = 0)
    var lastMonthRevenue: BigDecimal? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    // OwnerId polymorphic R-2 (referenceTo = [Group, User]) — sfid prefix `005` = User / `00G` = Group.
    // XOR CHECK 제약 chk_display_work_schedule_owner_xor.

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
