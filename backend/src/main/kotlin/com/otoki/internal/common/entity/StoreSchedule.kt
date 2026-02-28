package com.otoki.internal.common.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 거래처 일정 Entity (진열마스터 확정 스케줄)
 * V1 스키마: displayworkschedulemaster__c
 */
@Entity
@Table(name = "displayworkschedulemaster__c")
class StoreSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "account__c", length = 18)
    val account: String? = null,

    @Column(name = "fullname__c", length = 18)
    val fullName: String? = null,

    @Column(name = "startdate__c")
    val startDate: LocalDate? = null,

    @Column(name = "enddate__c")
    val endDate: LocalDate? = null,

    @Column(name = "confirmed__c")
    val confirmed: Boolean? = null,

    @Column(name = "typeofwork1__c", length = 255)
    val typeOfWork1: String? = null,

    @Column(name = "typeofwork3__c", length = 255)
    val typeOfWork3: String? = null,

    @Column(name = "typeofwork5__c", length = 255)
    val typeOfWork5: String? = null,

    @Column(name = "createdbyid", length = 18)
    val createdById: String? = null,

    @Column(name = "ownerid", length = 18)
    val ownerId: String? = null,

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
    // userId: Long — V1에서 fullName(String sfid)로 대체
    // storeId: Long — V1에서 account(String sfid)로 대체
    // storeName: String — V1에 없음
    // storeCode: String — V1에 없음
    // address: String? — V1에 없음
    // workCategory: String — V1에서 typeOfWork1로 대체
    // scheduleDate: LocalDate — V1에서 startDate로 대체
)
