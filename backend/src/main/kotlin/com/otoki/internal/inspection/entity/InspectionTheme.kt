package com.otoki.internal.inspection.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현장 점검 테마 Entity
 * V1 스키마: theme__c
 */
@Entity
@Table(name = "theme__c")
class InspectionTheme(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "title__c", length = 250)
    val title: String? = null,

    @Column(name = "startdate__c")
    val startDate: LocalDate? = null,

    @Column(name = "enddate__c")
    val endDate: LocalDate? = null,

    @Column(name = "department__c", length = 100)
    val department: String? = null,

    @Column(name = "branchcode__c", length = 30)
    val branchCode: String? = null,

    @Column(name = "publicflag__c")
    val publicFlag: Boolean? = null,

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

    // --- 주석 처리: V2 기존 필드 ---
    // isActive: Boolean — V1에서 publicFlag로 대체
    // createdAt: LocalDateTime — V1에서 createdDate로 대체
)
