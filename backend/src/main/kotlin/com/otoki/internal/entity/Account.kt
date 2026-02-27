package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 거래처 마스터 Entity
 * Salesforce Account(거래처) 오브젝트 — Heroku Connect로 동기화되는 거래처 마스터 테이블.
 * StoreSchedule의 storeId를 통해 연결된다.
 */
@Entity
@Table(name = "account")
class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 255)
    val name: String? = null,

    @Column(name = "phone", length = 40)
    val phone: String? = null,

    @Column(name = "mobilephone__c", length = 40)
    val mobilePhone: String? = null,

    @Column(name = "address1__c", length = 120)
    val address1: String? = null,

    @Column(name = "address2__c", length = 120)
    val address2: String? = null,

    @Column(name = "representative__c", length = 100)
    val representative: String? = null,

    @Column(name = "abctype__c", length = 20)
    val abcType: String? = null,

    @Column(name = "abctypecode__c", length = 40)
    val abcTypeCode: String? = null,

    @Column(name = "externalkey__c", length = 100)
    val externalKey: String? = null,

    @Column(name = "accountgroup__c", length = 10)
    val accountGroup: String? = null,

    @Column(name = "branchcode__c", length = 100)
    val branchCode: String? = null,

    @Column(name = "branchname__c", length = 250)
    val branchName: String? = null,

    @Column(name = "zipcode__c", length = 100)
    val zipCode: String? = null,

    @Column(name = "latitude__c", length = 100)
    val latitude: String? = null,

    @Column(name = "longitude__c", length = 100)
    val longitude: String? = null,

    @Column(name = "closingtime1__c", length = 50)
    val closingTime1: String? = null,

    @Column(name = "closingtime2__c", length = 50)
    val closingTime2: String? = null,

    @Column(name = "closingtime3__c", length = 50)
    val closingTime3: String? = null,

    @Column(name = "industry", length = 255)
    val industry: String? = null,

    @Column(name = "werk1_tx__c", length = 255)
    val werk1Tx: String? = null,

    @Column(name = "werk2_tx__c", length = 255)
    val werk2Tx: String? = null,

    @Column(name = "werk3_tx__c", length = 255)
    val werk3Tx: String? = null,

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

    // --- 주석 처리: V1에 없는 기존 필드 ---
    // storeCode: V1에 없음 (externalKey로 대체)
    // creditLimit: V1에 없음
    // usedCredit: V1에 없음
    // creditUpdatedAt: V1에 없음
)
