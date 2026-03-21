package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 푸시 메시지 수신자 Entity
 * V1 스키마: push_message_receiver (V42 리네이밍)
 *
 * PushMessage N:1 관계 (PK 참조, DB FK 없음)
 */
@Entity
@Table(name = "push_message_receiver")
@HCTable("pushmessagereceiver__c")
@SFObject("PushMessageReceiver__c")
class PushMessageReceiver(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("employeeid__c")
    @SFField("EmployeeId__c")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("messageid__c")
    @SFField("MessageId__c")
    @Column(name = "message_id")
    val messageId: Int? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
