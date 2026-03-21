package com.otoki.internal.common.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "agreement_history")
@SFObject("AgreementHistory__c")
class AgreementHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @SFField("EmployeeId__c")
    @Column(name = "employeeid__c", nullable = false)
    val employeeId: Long,

    @SFField("AgreementFlag__c")
    @Column(name = "agreementflag__c", nullable = false)
    val agreementFlag: Boolean,

    @SFField("AgreementDate__c")
    @Column(name = "agreementdate__c", nullable = false)
    val agreementDate: LocalDate,

    @SFField("AgreementWordId__c")
    @Column(name = "agreementwordid__c", nullable = false)
    val agreementWordId: Long,

    @Column(name = "isdeleted", nullable = false)
    val isDeleted: Boolean = false
) : BaseEntity()
