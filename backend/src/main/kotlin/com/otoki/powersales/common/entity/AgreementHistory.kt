package com.otoki.powersales.common.entity

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

    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_word_id", insertable = false, updatable = false)
    val agreementWord: AgreementWord? = null
) : BaseEntity()
