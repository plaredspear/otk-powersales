package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
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
class PushMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("message__c")
    @Column(name = "message__c", length = 500)
    val message: String? = null,

    @HCColumn("scheduledate__c")
    @Column(name = "scheduledate__c")
    val scheduleDate: LocalDateTime? = null,

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
