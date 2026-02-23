package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 제품 바코드 Entity
 * V1 스키마 productbarcode__c 테이블에 매핑.
 * Product와 1:N 관계 (sfid 문자열 참조, FK 없음).
 */
@Entity
@Table(name = "productbarcode__c")
class ProductBarcode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "productname__c", length = 255)
    val productName: String? = null,

    @Column(name = "productbarcode__c", length = 255)
    val productBarcode: String? = null,

    @Column(name = "productunit__c", length = 255)
    val productUnit: String? = null,

    @Column(name = "productsequence__c", length = 255)
    val productSequence: String? = null,

    @Column(name = "product__c", length = 18)
    val product: String? = null,

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
