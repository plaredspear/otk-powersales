package com.otoki.powersales.common.entity

import com.otoki.powersales.common.entity.converter.EvaluationTypeConverter
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 본사 평가 Entity
 * Salesforce HQReview__c (본부평가) — Spec #708 SF Object 정합 (Group A + Reference R-2).
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

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("BranchCode__c")
    @HCColumn("branchcode__c")
    @Column(name = "branch_code", length = 100)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    @Column(name = "branch_name", length = 100)
    var branchName: String? = null,

    @SFField("FirstDayofMonth__c")
    @HCColumn("firstdayofmonth__c")
    @Column(name = "first_day_of_month")
    var firstDayOfMonth: LocalDate? = null,

    @SFField("EvaluationyType__c")
    @HCColumn("evaluationytype__c")
    @Convert(converter = EvaluationTypeConverter::class)
    @Column(name = "evaluation_type", length = 255)
    var evaluationType: EvaluationType? = null,

    @SFField("ABCTypeCode__c")
    @HCColumn("abctypecode__c")
    @Column(name = "abc_type_code", length = 255)
    var abcTypeCode: String? = null,

    @SFField("HR_Code_c__c")
    @HCColumn("hr_code_c__c")
    @Column(name = "hr_code", length = 255)
    var hrCode: String? = null,

    // -- Spec #708: Group A — IsDeleted --
    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Spec #708: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑 결과 FK.

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

    // -- Relations --

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
