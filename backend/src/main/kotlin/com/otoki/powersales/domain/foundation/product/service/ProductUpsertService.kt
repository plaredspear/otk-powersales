package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.ProductType
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ParseResult
import com.otoki.powersales.domain.foundation.product.service.dto.ProductUpsertCommand
import com.otoki.powersales.domain.foundation.product.service.dto.ProductUpsertFailedRow
import com.otoki.powersales.domain.foundation.product.service.dto.ProductUpsertResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 제품 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapProductMasterService]
 * - origin spec: #559 (SAP 제품 마스터 인바운드) + #575 (레거시 필드 보존) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<ProductUpsertCommand>` — UPSERT 키 [ProductUpsertCommand.productCode].
 * 2. 캐시 빌드: [ProductRepository.findByProductCodeIn].
 * 3. 행 단위 검증/변환/적용:
 *    - 필수값 (`productCode`/`productName`) 누락 → failures.
 *    - 숫자 변환 (StandardPrice/BoxReceivingQuantity/SuperTax/Pallet) 실패 → failures.
 *    - 날짜 변환 (LaunchDate, `yyyyMMdd`. `null`/`""`/`"00000000"` → `2999-12-31` 센티넬) 형식 오류 → failures.
 *    - 정상 행: 신규 [Product] 생성 또는 기존 entity 의 mutable 필드 갱신.
 * 4. 외부 호출: [ProductRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class ProductUpsertService(
    private val productRepository: ProductRepository
) {

    @Transactional
    fun upsert(commands: List<ProductUpsertCommand>): ProductUpsertResult {
        val productCodes = commands.mapNotNull { it.productCode?.takeIf { code -> code.isNotBlank() } }
        val cache: MutableMap<String, Product> = if (productCodes.isEmpty()) {
            mutableMapOf()
        } else {
            productRepository.findByProductCodeIn(productCodes.distinct())
                .mapNotNull { p -> p.productCode?.let { it to p } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<ProductUpsertFailedRow>()
        val toSave = mutableListOf<Product>()

        commands.forEach { command ->
            val productCode = command.productCode?.takeIf { it.isNotBlank() }
            if (productCode == null) {
                failures += ProductUpsertFailedRow(null, "ProductCode 필수")
                return@forEach
            }
            val productName = command.productName?.takeIf { it.isNotBlank() }
            if (productName == null) {
                failures += ProductUpsertFailedRow(productCode, "ProductName 필수")
                return@forEach
            }

            val standardPrice = parseDouble(command.standardPrice, allowBlankAsNull = true)
            if (standardPrice.isFailure) {
                failures += ProductUpsertFailedRow(productCode, "StandardPrice 변환 실패: ${command.standardPrice}")
                return@forEach
            }
            val boxQty = parseDouble(command.boxReceivingQuantity, allowBlankAsNull = true)
            if (boxQty.isFailure) {
                failures += ProductUpsertFailedRow(
                    productCode,
                    "BoxReceivingQuantity 변환 실패: ${command.boxReceivingQuantity}"
                )
                return@forEach
            }
            val superTax = parseDouble(command.superTax, allowBlankAsZero = true)
            if (superTax.isFailure) {
                failures += ProductUpsertFailedRow(productCode, "SuperTax 변환 실패: ${command.superTax}")
                return@forEach
            }
            val launchDate = parseLaunchDate(command.launchDate)
            if (launchDate.isFailure) {
                failures += ProductUpsertFailedRow(productCode, "LaunchDate 형식 오류: ${command.launchDate}")
                return@forEach
            }
            val pallet = parseDouble(command.pallet, allowBlankAsZero = true)
            if (pallet.isFailure) {
                failures += ProductUpsertFailedRow(productCode, "Pallet 형식 오류: ${command.pallet}")
                return@forEach
            }

            val entity = cache[productCode]
                ?.also { applyToEntity(it, command, productName, standardPrice.value, boxQty.value, superTax.value, launchDate.value, pallet.value) }
                ?: createProduct(productCode, productName, command, standardPrice.value, boxQty.value, superTax.value, launchDate.value, pallet.value)
                    .also { cache[productCode] = it }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            productRepository.saveAll(toSave)
        }

        return ProductUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun createProduct(
        productCode: String,
        productName: String,
        command: ProductUpsertCommand,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ): Product {
        val product = Product(productCode = productCode, name = productName)
        applyMutableFields(product, command, standardPrice, boxQty, superTax, launchDate, pallet)
        return product
    }

    private fun applyToEntity(
        product: Product,
        command: ProductUpsertCommand,
        productName: String,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ) {
        product.name = productName
        applyMutableFields(product, command, standardPrice, boxQty, superTax, launchDate, pallet)
    }

    private fun applyMutableFields(
        product: Product,
        command: ProductUpsertCommand,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ) {
        product.productStatus = ProductStatus.Companion.fromDisplayNameOrNull(command.productStatus)
        product.productType = ProductType.Companion.fromDisplayNameOrNull(command.productType)
        product.productCategory1 = command.category1
        product.productCategory2 = command.category2
        product.productCategory3 = command.category3
        product.categoryCode1 = command.categoryCode1
        product.categoryCode2 = command.categoryCode2
        product.categoryCode3 = command.categoryCode3
        product.unit = command.unit
        product.shelfLife = command.shelfLife
        product.shelfLifeUnit = command.shelfLifeUnit
        product.tasteGift = command.tasteGift
        product.logisticsBarcode = command.logisticsBarCode
        product.storageCondition = StorageCondition.Companion.fromDisplayNameOrNull(command.storeCondition)
        product.standardUnitPrice = standardPrice?.let { BigDecimal.valueOf(it) }
        product.boxReceivingQuantity = boxQty?.let { BigDecimal.valueOf(it) }
        product.superTax = BigDecimal.valueOf(superTax ?: 0.0)
        product.launchDate = launchDate
        // Spec #575: SAP ProductBarcode 수신값.
        // 레거시 IF_REST_SAP_ProductMasterSend 는 ProductBarcode → DKRetail__Barcode__c 단일 적재.
        // 신규도 동일하게 DKRetail__Barcode__c 매핑 컬럼(barcode, #613)에 적재 — admin 제품 상세 barcode 노출 정합.
        product.barcode = command.productBarcode
        product.pallet = pallet?.let { BigDecimal.valueOf(it) }
    }

    private fun parseDouble(
        value: String?,
        allowBlankAsNull: Boolean = false,
        allowBlankAsZero: Boolean = false
    ): ParseResult<Double> {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty()) {
            return when {
                allowBlankAsZero -> ParseResult(0.0, false)
                allowBlankAsNull -> ParseResult(null, false)
                else -> ParseResult(null, true)
            }
        }
        return try {
            ParseResult(trimmed.toDouble(), false)
        } catch (_: NumberFormatException) {
            ParseResult(null, true)
        }
    }

    /**
     * SF `Util.convertStringToDate` 정합 — 빈값/`null`/`"00000000"` → `2999-12-31` 센티넬 (발매일 미정).
     * 형식 오류(8자리 yyyyMMdd 가 아님) 는 행 failure (`isFailure=true`).
     */
    private fun parseLaunchDate(value: String?): ParseResult<LocalDate> {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed == "00000000") {
            return ParseResult(DATE_SENTINEL, false)
        }
        return try {
            ParseResult(LocalDate.parse(trimmed, DATE_FORMAT), false)
        } catch (_: DateTimeParseException) {
            ParseResult(null, true)
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        // SF Util.convertStringToDate 빈값/00000000 센티넬 (발매일 미정). ErpOrder·employee 와 동일 정합.
        private val DATE_SENTINEL: LocalDate = LocalDate.of(2999, 12, 31)
    }
}
