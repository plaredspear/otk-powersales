package com.otoki.powersales.common.entity

import com.otoki.powersales.common.entity.converter.PushMessageBranchCodeConverter
import com.otoki.powersales.common.entity.converter.PushMessageBranchConverter
import com.otoki.powersales.common.enums.PushMessageBranch
import com.otoki.powersales.common.enums.PushMessageBranchCode
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 푸시 메시지 Entity
 * Salesforce PushMessage__c (메시지) — Spec #709 SF Object 정합 (Group A + Reference R-2).
 */
@Entity
@Table(name = "push_message")
@SFObject("PushMessage__c")
class PushMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_message_id")
    val id: Int = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Message__c")
    @Column(name = "message", length = 500)
    val message: String? = null,

    @SFField("ScheduleDate__c")
    @Column(name = "schedule_date")
    val scheduleDate: LocalDateTime? = null,

    // --- Spec #615: SF 누락 비수식 4개 도입 ---

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("Branch__c")
    @Convert(converter = PushMessageBranchConverter::class)
    @Column(name = "branch", length = 100)
    val branch: PushMessageBranch? = null,

    @SFField("BranchCode__c")
    @Convert(converter = PushMessageBranchCodeConverter::class)
    @Column(name = "branch_code", length = 40)
    val branchCode: PushMessageBranchCode? = null,

    @SFField("SObjectRecordId__c")
    @Column(name = "s_object_record_id", length = 50)
    val sObjectRecordId: String? = null,

    // -- Spec #709: Group A — IsDeleted --
    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #709: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑 결과 FK.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null

) : BaseEntity()
