package com.otoki.powersales.domain.foundation.product.service.dto

/**
 * 제품 마스터 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: [productCode] (= [com.otoki.powersales.domain.foundation.product.entity.Product.productCode])
 *
 * 외부 채널(SAP 인바운드 등) 의 페이로드를 [com.otoki.powersales.domain.foundation.product.service.ProductUpsertService] 가 받기 위한 도메인 용어 모델.
 * 숫자/날짜 필드는 도메인 측이 String 으로 받아 도메인에서 변환 + 행 단위 검증 분기.
 */
data class ProductUpsertCommand(
    val productCode: String?,
    val productName: String?,
    val productBarcode: String?,
    val logisticsBarCode: String?,
    val categoryCode1: String?,
    val category1: String?,
    val categoryCode2: String?,
    val category2: String?,
    val categoryCode3: String?,
    val category3: String?,
    val productStatus: String?,
    val standardPrice: String?,
    val unit: String?,
    val boxReceivingQuantity: String?,
    val shelfLife: String?,
    val shelfLifeUnit: String?,
    val launchDate: String?,
    val storeCondition: String?,
    val productType: String?,
    val superTax: String?,
    val tasteGift: String?,
    val pallet: String?
)
