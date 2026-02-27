package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * SF 동기화 파일 메타데이터 Entity (uploadfile__c 테이블)
 *
 * auto-increment(serial4) 단일 PK. 읽기 위주.
 */
@Entity
@Table(name = "uploadfile__c")
class UploadFile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "recordid__c", length = 40)
    val recordId: String? = null,

    @Column(name = "uniquekey__c", length = 100)
    val uniqueKey: String? = null,

    @Column(name = "size__c", length = 100)
    val size: String? = null,

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
