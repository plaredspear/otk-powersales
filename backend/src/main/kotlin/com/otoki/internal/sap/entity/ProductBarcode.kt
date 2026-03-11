package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 제품 바코드 Entity
 * V1 스키마 productbarcode__c 테이블에 매핑.
 * Product와 1:N 관계 (sfid 문자열 참조, FK 없음).
 */
@Entity
@Table(name = "productbarcode__c")
@SFObject("ProductBarcode__c")
@HCTable("productbarcode__c")
class ProductBarcode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    @HCColumn("sfid")
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    @SFField("Name")
    @HCColumn("name")
    var name: String? = null,

    @Column(name = "productname__c", length = 255)
    @SFField("ProductName__c")
    @HCColumn("productname__c")
    var productName: String? = null,

    @Column(name = "productbarcode__c", length = 255)
    @SFField("ProductBarcode__c")
    @HCColumn("productbarcode__c")
    var productBarcode: String? = null,

    @Column(name = "productunit__c", length = 255)
    @SFField("ProductUnit__c")
    @HCColumn("productunit__c")
    var productUnit: String? = null,

    @Column(name = "productsequence__c", length = 255)
    @SFField("ProductSequence__c")
    @HCColumn("productsequence__c")
    var productSequence: String? = null,

    @Column(name = "product__c", length = 18)
    @SFField("Product__c")
    @HCColumn("product__c")
    var product: String? = null,

    @Column(name = "custom_key__c", length = 255)
    @SFField("CustomKey__c")
    var customKey: String? = null,

    @Column(name = "isdeleted")
    @HCColumn("isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    @HCColumn("createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    @HCColumn("systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    @HCColumn("_hc_lastop")
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    @HCColumn("_hc_err")
    val hcErr: String? = null
)
