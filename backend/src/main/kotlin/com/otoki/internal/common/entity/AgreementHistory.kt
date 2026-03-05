package com.otoki.internal.common.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "agreementhistory__c")
class AgreementHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "employeeid__c", nullable = false)
    val employeeId: Long,

    @Column(name = "agreementflag__c", nullable = false)
    val agreementFlag: Boolean,

    @Column(name = "agreementdate__c", nullable = false)
    val agreementDate: LocalDate,

    @Column(name = "agreementwordid__c", nullable = false)
    val agreementWordId: Long,

    @Column(name = "createddate", nullable = false)
    val createdDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "isdeleted", nullable = false)
    val isDeleted: Boolean = false
)
