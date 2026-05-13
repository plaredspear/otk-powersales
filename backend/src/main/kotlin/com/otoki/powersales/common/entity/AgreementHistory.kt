package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "agreement_history")
@SFObject("AgreementHistory__c")
class AgreementHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("AgreementFlag__c")
    @Column(name = "agreement_flag", nullable = false)
    val agreementFlag: Boolean,

    @SFField("AgreementDate__c")
    @Column(name = "agreement_date", nullable = false)
    val agreementDate: LocalDate,

    @Column(name = "agreement_word_id", nullable = false)
    val agreementWordId: Long,

    @SFField("AgreementWordId__c")
    @Column(name = "agreement_word_sfid", length = 18)
    val agreementWordSfid: String? = null,

    // -- Spec #706: Group A — IsDeleted --
    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Spec #706: 표준 비-A 필드 (Name = 동의번호) --
    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    // -- Spec #706: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (SalesforceMigrationTool 이 채움).
    // *_id / owner: SF User → Employee 매핑 결과 FK.

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
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_word_id", insertable = false, updatable = false)
    val agreementWord: AgreementWord? = null,

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
