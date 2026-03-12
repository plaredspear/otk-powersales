package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*

/**
 * 제품 바코드 Entity
 * V1 스키마 product_barcode 테이블에 매핑.
 * Product와 1:N 관계 (sfid 문자열 참조, FK 없음).
 */
@Entity
@Table(name = "product_barcode")
@SFObject("ProductBarcode__c")
@HCTable("productbarcode__c")
class ProductBarcode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("ProductName__c")
    @HCColumn("productname__c")
    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @SFField("ProductBarcode__c")
    @HCColumn("productbarcode__c")
    @Column(name = "barcode", length = 255)
    var barcode: String? = null,

    @SFField("ProductUnit__c")
    @HCColumn("productunit__c")
    @Column(name = "unit", length = 255)
    var unit: String? = null,

    @SFField("ProductSequence__c")
    @HCColumn("productsequence__c")
    @Column(name = "sort_order", length = 255)
    var sortOrder: String? = null,

    @SFField("Product__c")
    @HCColumn("product__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    @SFField("CustomKey__c")
    @Column(name = "custom_key", length = 255)
    var customKey: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null
)
