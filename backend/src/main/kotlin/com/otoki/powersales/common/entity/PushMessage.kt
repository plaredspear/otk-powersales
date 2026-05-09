package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
/**
 * 푸시 메시지 Entity
 * V1 스키마: pushmessage__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "push_message")
@HCTable("pushmessage__c")
@SFObject("PushMessage__c")
class PushMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_message_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("message__c")
    @SFField("Message__c")
    @Column(name = "message", length = 500)
    val message: String? = null,

    @HCColumn("scheduledate__c")
    @SFField("ScheduleDate__c")
    @Column(name = "schedule_date")
    val scheduleDate: LocalDateTime? = null,

    // --- Spec #615: SF 누락 비수식 4개 도입 ---

    @SFField("EmployeeId__c")
    @HCColumn("employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("Branch__c")
    @HCColumn("branch__c")
    @Column(name = "branch", length = 100)
    val branch: String? = null,

    @SFField("BranchCode__c")
    @HCColumn("branchcode__c")
    @Column(name = "branch_code", length = 40)
    val branchCode: String? = null,

    @SFField("SObjectRecordId__c")
    @HCColumn("sobjectrecordid__c")
    @Column(name = "s_object_record_id", length = 50)
    val sObjectRecordId: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()