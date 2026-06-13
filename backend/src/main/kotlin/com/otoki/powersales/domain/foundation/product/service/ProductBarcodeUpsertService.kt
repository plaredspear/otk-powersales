package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertCommand
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertFailedRow
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 제품 바코드 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapBarcodeMasterService]
 * - origin spec: #559 (SAP 제품 마스터 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<ProductBarcodeUpsertCommand>` — UPSERT 키 (`productCode + productUnit + productSequence` 단순 연결).
 * 2. 캐시 빌드: [ProductRepository.findByProductCodeIn] (FK lookup) / [ProductBarcodeRepository.findByCustomKey] (개별 조회 — 기존 동작 보존).
 * 3. 행 단위 검증/적용:
 *    - 필수값 (`productCode`/`productUnit`/`productSequence`/`productBarcode`) 누락 → failures.
 *    - Product 매칭 실패 (`product_code not found: <code>`) → failures.
 *    - 정상 행: 신규 [ProductBarcode] 생성 또는 기존 entity 의 mutable 필드 갱신.
 * 4. 외부 호출: [ProductBarcodeRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class ProductBarcodeUpsertService(
    private val productBarcodeRepository: ProductBarcodeRepository,
    private val productRepository: ProductRepository
) {

    @Transactional
    fun upsert(commands: List<ProductBarcodeUpsertCommand>): ProductBarcodeUpsertResult {
        val productCodes = commands.mapNotNull { it.productCode?.takeIf { code -> code.isNotBlank() } }
        val productCache: Map<String, Product> = if (productCodes.isEmpty()) {
            emptyMap()
        } else {
            productRepository.findByProductCodeIn(productCodes.distinct())
                .mapNotNull { p -> p.productCode?.let { it to p } }
                .toMap()
        }

        val customKeys = commands.mapNotNull { customKey(it) }
        val barcodeCache: MutableMap<String, ProductBarcode> = if (customKeys.isEmpty()) {
            mutableMapOf()
        } else {
            customKeys.distinct()
                .mapNotNull { key -> productBarcodeRepository.findByCustomKey(key)?.let { key to it } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<ProductBarcodeUpsertFailedRow>()
        val toSave = mutableListOf<ProductBarcode>()

        commands.forEach { command ->
            val productCode = command.productCode?.takeIf { it.isNotBlank() }
            val productUnit = command.productUnit?.takeIf { it.isNotBlank() }
            val productSequence = command.productSequence?.takeIf { it.isNotBlank() }
            val barcode = command.productBarcode?.takeIf { it.isNotBlank() }

            if (productCode == null) {
                failures += ProductBarcodeUpsertFailedRow(null, "ProductCode 필수")
                return@forEach
            }
            if (productUnit == null) {
                failures += ProductBarcodeUpsertFailedRow(null, "ProductUnit 필수")
                return@forEach
            }
            if (productSequence == null) {
                failures += ProductBarcodeUpsertFailedRow(null, "ProductSequence 필수")
                return@forEach
            }
            if (barcode == null) {
                failures += ProductBarcodeUpsertFailedRow(
                    productCode + productUnit + productSequence,
                    "ProductBarcode 필수"
                )
                return@forEach
            }

            val key = productCode + productUnit + productSequence
            val matchedProduct = productCache[productCode]
            if (matchedProduct == null) {
                failures += ProductBarcodeUpsertFailedRow(key, "product_code not found: $productCode")
                return@forEach
            }

            val entity = barcodeCache[key]?.also {
                applyToEntity(it, command, key, matchedProduct, productUnit, barcode)
            } ?: createBarcode(key, command, matchedProduct, productUnit, barcode)
                .also { barcodeCache[key] = it }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            productBarcodeRepository.saveAll(toSave)
        }

        return ProductBarcodeUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun customKey(command: ProductBarcodeUpsertCommand): String? {
        val pc = command.productCode?.takeIf { it.isNotBlank() } ?: return null
        val pu = command.productUnit?.takeIf { it.isNotBlank() } ?: return null
        val ps = command.productSequence?.takeIf { it.isNotBlank() } ?: return null
        return pc + pu + ps
    }

    private fun createBarcode(
        key: String,
        command: ProductBarcodeUpsertCommand,
        matchedProduct: Product,
        productUnit: String,
        barcode: String
    ): ProductBarcode {
        val entity = ProductBarcode(
            customKey = key,
            name = productUnit,
            unit = productUnit,
            barcode = barcode,
            productId = matchedProduct.id
        )
        entity.productName = command.productName
        entity.sortOrder = command.productSequence
        return entity
    }

    private fun applyToEntity(
        entity: ProductBarcode,
        command: ProductBarcodeUpsertCommand,
        key: String,
        matchedProduct: Product,
        productUnit: String,
        barcode: String
    ) {
        entity.customKey = key
        entity.name = productUnit
        entity.unit = productUnit
        entity.barcode = barcode
        entity.sortOrder = command.productSequence
        entity.productName = command.productName
        // sfid 는 SF 데이터 마이그레이션 보조 필드 — runtime 에서 박지 않음 (정책).
        entity.productId = matchedProduct.id
    }
}
