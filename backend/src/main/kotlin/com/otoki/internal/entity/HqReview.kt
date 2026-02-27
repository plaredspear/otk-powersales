package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 본사 평가 Entity
 * V1 스키마: hqreview__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "hqreview__c")
class HqReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "branchcode__c", length = 100)
    val branchCode: String? = null,

    @Column(name = "branchname__c", length = 100)
    val branchName: String? = null,

    @Column(name = "firstdayofmonth__c")
    val firstDayOfMonth: LocalDate? = null,

    @Column(name = "evaluationytype__c", length = 255)
    val evaluationType: String? = null,

    @Column(name = "abctypecode__c", length = 255)
    val abcTypeCode: String? = null,

    @Column(name = "hr_code_c__c", length = 255)
    val hrCode: String? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null
)
