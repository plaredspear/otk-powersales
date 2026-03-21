package com.otoki.internal.common.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "agreement_word")
@HCTable("agreementword__c")
@SFObject("AgreementWord__c")
class AgreementWord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_word_id")
    val id: Int = 0,

    @HCColumn("name")
    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("contents__c")
    @SFField("Contents__c")
    @Column(name = "contents", length = 8000)
    val contents: String? = null,

    @HCColumn("active__c")
    @SFField("Active__c")
    @Column(name = "active")
    val active: Boolean? = null,

    @HCColumn("activedate__c")
    @SFField("ActiveDate__c")
    @Column(name = "active_date")
    val activeDate: LocalDate? = null,

    @HCColumn("afteractivedate__c")
    @SFField("AfterActiveDate__c")
    @Column(name = "after_active_date")
    val afterActiveDate: LocalDate? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
