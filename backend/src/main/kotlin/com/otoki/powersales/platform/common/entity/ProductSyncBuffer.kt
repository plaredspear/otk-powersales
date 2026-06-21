package com.otoki.powersales.platform.common.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
/**
 * PLM 연동용 제품 동기화 버퍼 Entity.
 * product 테이블의 트리거(if__product)가 INSERT 시 id를 복사하므로 외부 할당 PK.
 * 읽기 전용 — 앱에서 INSERT/UPDATE 하지 않음.
 */
@DomainName("제품동기화버퍼")
@Entity
@Table(name = "product_sync_buffer")
@HerokuOnly("if_product__c")
class ProductSyncBuffer(

    @Id
    @FieldName("제품동기화버퍼ID")
    @Column(name = "product_sync_buffer_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("dkretail__productcode__c")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @HCColumn("dkretail__producttype__c")
    @FieldName("상품유형")
    @Column(name = "product_type", length = 255)
    val productType: String? = null,

    @HCColumn("dkretail__productstatus__c")
    @FieldName("제품상태")
    @Column(name = "product_status", length = 255)
    val productStatus: String? = null,

    @HCColumn("dkretail__storecondition__c")
    @FieldName("보관방법")
    @Column(name = "storage_condition", length = 255)
    val storageCondition: String? = null,

    @HCColumn("dkretail__shelflife__c")
    @FieldName("유통기한")
    @Column(name = "shelf_life", length = 30)
    val shelfLife: String? = null,

    @HCColumn("dkretail__shelflifeunit__c")
    @FieldName("유통기한단위")
    @Column(name = "shelf_life_unit", length = 40)
    val shelfLifeUnit: String? = null,

    @HCColumn("dkretail__category1__c")
    @FieldName("대분류")
    @Column(name = "category1", length = 255)
    val category1: String? = null,

    @HCColumn("dkretail__category2__c")
    @FieldName("중분류")
    @Column(name = "category2", length = 255)
    val category2: String? = null,

    @HCColumn("dkretail__category3__c")
    @FieldName("소분류")
    @Column(name = "category3", length = 255)
    val category3: String? = null,

    @HCColumn("dkretail__categorycode1__c")
    @FieldName("대분류코드")
    @Column(name = "category_code1", length = 100)
    val categoryCode1: String? = null,

    @HCColumn("dkretail__categorycode2__c")
    @FieldName("중분류코드")
    @Column(name = "category_code2", length = 100)
    val categoryCode2: String? = null,

    @HCColumn("dkretail__categorycode3__c")
    @FieldName("소분류코드")
    @Column(name = "category_code3", length = 100)
    val categoryCode3: String? = null,

    @HCColumn("dkretail__unit__c")
    @FieldName("단위")
    @Column(name = "unit", length = 40)
    val unit: String? = null,

    @HCColumn("dkretail__orderingunit__c")
    @FieldName("발주단위")
    @Column(name = "ordering_unit", length = 40)
    val orderingUnit: String? = null,

    @HCColumn("dkretail__conversionquantity__c")
    @FieldName("환산수량")
    @Column(name = "conversion_quantity")
    val conversionQuantity: Double? = null,

    @HCColumn("dkretail__boxreceivingquantity__c")
    @FieldName("박스입수량")
    @Column(name = "box_receiving_quantity")
    val boxReceivingQuantity: Double? = null,

    @HCColumn("dkretail__standardunitprice__c")
    @FieldName("표준가")
    @Column(name = "standard_unit_price")
    val standardUnitPrice: Double? = null,

    @HCColumn("supertax__c")
    @FieldName("부가세")
    @Column(name = "super_tax")
    val superTax: Double? = null,

    @HCColumn("dkretail__launchdate__c")
    @FieldName("출시일")
    @Column(name = "launch_date")
    val launchDate: LocalDate? = null,

    @HCColumn("dkretail__logisticsbarcode__c")
    @FieldName("물류바코드")
    @Column(name = "logistics_barcode", length = 100)
    val logisticsBarcode: String? = null,

    @HCColumn("tastegift__c")
    @FieldName("증정/시식 구분")
    @Column(name = "taste_gift", length = 1)
    val tasteGift: String? = null,

    @HCColumn("productfeatures__c")
    @FieldName("제품특징")
    @Column(name = "product_features", length = 255)
    val productFeatures: String? = null,

    @HCColumn("sellingpoint__c")
    @FieldName("셀링포인트")
    @Column(name = "selling_point", length = 255)
    val sellingPoint: String? = null,

    @HCColumn("purpose__c")
    @FieldName("용도")
    @Column(name = "purpose", length = 255)
    val purpose: String? = null,

    @HCColumn("targetaccounttype__c")
    @FieldName("타겟거래처유형")
    @Column(name = "target_account_type", length = 255)
    val targetAccountType: String? = null,

    @HCColumn("allergen__c")
    @FieldName("알러지 유발물질")
    @Column(name = "allergen", length = 255)
    val allergen: String? = null,

    @HCColumn("crosscontamination__c")
    @FieldName("교차오염")
    @Column(name = "cross_contamination", length = 255)
    val crossContamination: String? = null,

    @HCColumn("imgrefpath__c")
    @FieldName("제품이미지 참조경로")
    @Column(name = "img_ref_path", length = 255)
    val imgRefPath: String? = null,

    @HCColumn("imgrefpath_front__c")
    @FieldName("제품이미지 참조경로_전면")
    @Column(name = "img_ref_path_front", length = 255)
    val imgRefPathFront: String? = null,

    @HCColumn("imgrefpath_back__c")
    @FieldName("제품이미지 참조경로_후면")
    @Column(name = "img_ref_path_back", length = 255)
    val imgRefPathBack: String? = null,

    @HCColumn("imgrefpathtxt__c")
    @FieldName("제품이미지 참조경로_텍스트")
    @Column(name = "img_ref_path_txt", length = 255)
    val imgRefPathTxt: String? = null,

    @HCColumn("updateflag__c")
    @FieldName("갱신플래그")
    @Column(name = "update_flag")
    val updateFlag: Boolean? = null,

    @HCColumn("isdeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()