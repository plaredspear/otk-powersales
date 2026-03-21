package com.otoki.internal.common.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "agreement_word")
@HCTable("agreementword__c")
class AgreementWord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("contents__c")
    @Column(name = "contents__c", length = 8000)
    val contents: String? = null,

    @HCColumn("active__c")
    @Column(name = "active__c")
    val active: Boolean? = null,

    @HCColumn("activedate__c")
    @Column(name = "activedate__c")
    val activeDate: LocalDate? = null,

    @HCColumn("afteractivedate__c")
    @Column(name = "afteractivedate__c")
    val afterActiveDate: LocalDate? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
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
