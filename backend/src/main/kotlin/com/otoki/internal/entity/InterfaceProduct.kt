package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 인터페이스 제품 Entity
 * V1 스키마 if_product__c 테이블에 매핑.
 * dkretail__product__c의 트리거(if__product)가 INSERT 시 id를 복사하므로 외부 할당 PK.
 * 읽기 전용 — 앱에서 INSERT/UPDATE 하지 않음.
 */
@Entity
@Table(name = "if_product__c")
class InterfaceProduct(

    @Id
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "dkretail__productcode__c", length = 100)
    val productCode: String? = null,

    @Column(name = "dkretail__producttype__c", length = 255)
    val productType: String? = null,

    @Column(name = "dkretail__productstatus__c", length = 255)
    val productStatus: String? = null,

    @Column(name = "dkretail__storecondition__c", length = 255)
    val storageCondition: String? = null,

    @Column(name = "dkretail__shelflife__c", length = 30)
    val shelfLife: String? = null,

    @Column(name = "dkretail__shelflifeunit__c", length = 40)
    val shelfLifeUnit: String? = null,

    @Column(name = "dkretail__category1__c", length = 255)
    val category1: String? = null,

    @Column(name = "dkretail__category2__c", length = 255)
    val category2: String? = null,

    @Column(name = "dkretail__category3__c", length = 255)
    val category3: String? = null,

    @Column(name = "dkretail__categorycode1__c", length = 100)
    val categoryCode1: String? = null,

    @Column(name = "dkretail__categorycode2__c", length = 100)
    val categoryCode2: String? = null,

    @Column(name = "dkretail__categorycode3__c", length = 100)
    val categoryCode3: String? = null,

    @Column(name = "dkretail__unit__c", length = 40)
    val unit: String? = null,

    @Column(name = "dkretail__orderingunit__c", length = 40)
    val orderingUnit: String? = null,

    @Column(name = "dkretail__conversionquantity__c")
    val conversionQuantity: Double? = null,

    @Column(name = "dkretail__boxreceivingquantity__c")
    val boxReceivingQuantity: Double? = null,

    @Column(name = "dkretail__standardunitprice__c")
    val standardUnitPrice: Double? = null,

    @Column(name = "supertax__c")
    val superTax: Double? = null,

    @Column(name = "dkretail__launchdate__c")
    val launchDate: LocalDate? = null,

    @Column(name = "dkretail__logisticsbarcode__c", length = 100)
    val logisticsBarcode: String? = null,

    @Column(name = "tastegift__c", length = 1)
    val tasteGift: String? = null,

    @Column(name = "productfeatures__c", length = 255)
    val productFeatures: String? = null,

    @Column(name = "sellingpoint__c", length = 255)
    val sellingPoint: String? = null,

    @Column(name = "purpose__c", length = 255)
    val purpose: String? = null,

    @Column(name = "targetaccounttype__c", length = 255)
    val targetAccountType: String? = null,

    @Column(name = "allergen__c", length = 255)
    val allergen: String? = null,

    @Column(name = "crosscontamination__c", length = 255)
    val crossContamination: String? = null,

    @Column(name = "imgrefpath__c", length = 255)
    val imgRefPath: String? = null,

    @Column(name = "imgrefpath_front__c", length = 255)
    val imgRefPathFront: String? = null,

    @Column(name = "imgrefpath_back__c", length = 255)
    val imgRefPathBack: String? = null,

    @Column(name = "imgrefpathtxt__c", length = 255)
    val imgRefPathTxt: String? = null,

    @Column(name = "updateflag__c")
    val updateFlag: Boolean? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

)
