package com.otoki.internal.common.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 본사 평가 Entity
 * V1 스키마: hqreview__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "hq_review")
@HCTable("hqreview__c")
@SFObject("HQReview__c")
class HqReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hq_review_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("branchcode__c")
    @SFField("BranchCode__c")
    @Column(name = "branch_code", length = 100)
    val branchCode: String? = null,

    @HCColumn("branchname__c")
    @SFField("BranchName__c")
    @Column(name = "branch_name", length = 100)
    val branchName: String? = null,

    @HCColumn("firstdayofmonth__c")
    @SFField("FirstDayofMonth__c")
    @Column(name = "first_day_of_month")
    val firstDayOfMonth: LocalDate? = null,

    @HCColumn("evaluationytype__c")
    @SFField("EvaluationyType__c")
    @Column(name = "evaluation_type", length = 255)
    val evaluationType: String? = null,

    @HCColumn("abctypecode__c")
    @SFField("ABCTypeCode__c")
    @Column(name = "abc_type_code", length = 255)
    val abcTypeCode: String? = null,

    @HCColumn("hr_code_c__c")
    @SFField("HR_Code_c__c")
    @Column(name = "hr_code", length = 255)
    val hrCode: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
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
