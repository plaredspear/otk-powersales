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

    @Column(name = "sfid", length = 18)
    @HCColumn("sfid")
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    @SFField("Name")
    @HCColumn("name")
    var name: String? = null,

    @Column(name = "product_name", length = 255)
    @SFField("ProductName__c")
    @HCColumn("productname__c")
    var productName: String? = null,

    @Column(name = "barcode", length = 255)
    @SFField("ProductBarcode__c")
    @HCColumn("productbarcode__c")
    var barcode: String? = null,

    @Column(name = "unit", length = 255)
    @SFField("ProductUnit__c")
    @HCColumn("productunit__c")
    var unit: String? = null,

    @Column(name = "sort_order", length = 255)
    @SFField("ProductSequence__c")
    @HCColumn("productsequence__c")
    var sortOrder: String? = null,

    @Column(name = "product_sfid", length = 18)
    @SFField("Product__c")
    @HCColumn("product__c")
    var productSfid: String? = null,

    @Column(name = "custom_key", length = 255)
    @SFField("CustomKey__c")
    var customKey: String? = null,

    @Column(name = "is_deleted")
    @HCColumn("isdeleted")
    val isDeleted: Boolean? = null
)
