package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 푸시 메시지 수신자 Entity
 * V1 스키마: pushmessagereceiver__c (Heroku Connect 동기화)
 *
 * PushMessage N:1 관계 (sfid 참조, DB FK 없음) → messageId는 raw String 컬럼
 */
@Entity
@Table(name = "pushmessagereceiver__c")
class PushMessageReceiver(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "employeeid__c", length = 18)
    val employeeId: String? = null,

    @Column(name = "messageid__c", length = 18)
    val messageId: String? = null,

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
