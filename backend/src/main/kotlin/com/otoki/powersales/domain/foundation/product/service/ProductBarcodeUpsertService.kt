package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertCommand
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertFailedRow
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 제품 바코드 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapBarcodeMasterService]
 * - origin spec: #559 (SAP 제품 마스터 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<ProductBarcodeUpsertCommand>` — UPSERT 키 `customKey = productCode + productUnit + productSequence` (단순 연결).
 * 2. 캐시 빌드: [ProductRepository.findByProductCodeIn] (FK lookup).
 * 3. 행 단위 적재: 레거시 `IF_REST_SAP_BarcodeMaster` 정합 — `ProductCode`/`ProductUnit`/`ProductSequence`/
 *    `ProductBarcode` 모두 SF `nillable=true` (`required=false`) 라 **명시 필수 검증을 두지 않고 raw 적재**한다.
 *    blank 는 null 로 정규화하되, customKey 합성은 레거시 `obj.ProductCode + obj.ProductUnit + obj.ProductSequence`
 *    동등하게 null 을 빈 문자열로 이어붙인다.
 *    - Product 매칭 실패 → orphan 저장 (productId=null, product_code 평문 보존). 레거시 SF 가 Product__c=null 로
 *      그대로 upsert 하는 동작 정합.
 *    - `custom_key` 는 SF external key 이자 신규 UNIQUE 제약이므로, 행마다 별도 트랜잭션([Propagation.REQUIRES_NEW])
 *      에서 flush 해 UNIQUE 충돌이 나면 그 행만 failure 로 격리한다 (레거시 `Database.upsert(CustomKey__c, false)` 동등).
 * 4. 외부 호출: [ProductBarcodeRowUpsertService.persistRow] (행 단위 saveAndFlush).
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class ProductBarcodeUpsertService(
    private val productRepository: ProductRepository,
    private val rowUpsertService: ProductBarcodeRowUpsertService
) {

    @Transactional(readOnly = true)
    fun upsert(commands: List<ProductBarcodeUpsertCommand>): ProductBarcodeUpsertResult {
        val productCodes = commands.mapNotNull { it.productCode?.takeIf { code -> code.isNotBlank() } }
        val productCache: Map<String, Product> = if (productCodes.isEmpty()) {
            emptyMap()
        } else {
            productRepository.findByProductCodeIn(productCodes.distinct())
                .mapNotNull { p -> p.productCode?.let { it to p } }
                .toMap()
        }

        val failures = mutableListOf<ProductBarcodeUpsertFailedRow>()
        var successCount = 0

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_BarcodeMaster 정합 — 네 필드 명시 검증 없이 raw 적재 (blank → null).
            val productCode = command.productCode?.takeIf { it.isNotBlank() }
            val productUnit = command.productUnit?.takeIf { it.isNotBlank() }
            val productSequence = command.productSequence?.takeIf { it.isNotBlank() }
            val barcode = command.productBarcode?.takeIf { it.isNotBlank() }

            // 레거시 `obj.ProductCode + obj.ProductUnit + obj.ProductSequence` 동등 — null 은 빈 문자열로 연결.
            val key = productCode.orEmpty() + productUnit.orEmpty() + productSequence.orEmpty()
            val matchedProduct = productCode?.let { productCache[it] }

            try {
                rowUpsertService.persistRow(command, key, matchedProduct, productCode, productUnit, barcode)
                successCount++
            } catch (ex: DataIntegrityViolationException) {
                failures += ProductBarcodeUpsertFailedRow(key, "적재 실패: ${ex.mostSpecificCauseText()}")
            }
        }

        return ProductBarcodeUpsertResult(
            successCount = successCount,
            failureCount = failures.size,
            failures = failures
        )
    }
}

/**
 * [ProductBarcodeUpsertService] 의 행 단위 트랜잭션 경계 빈.
 *
 * 한 요청 안의 여러 행 중 `custom_key` UNIQUE 충돌이 난 행만 격리하기 위해 행마다 [Propagation.REQUIRES_NEW]
 * 로 별도 트랜잭션을 연다 (같은 빈 내 self-invocation 은 Spring AOP 프록시를 우회해 트랜잭션 경계가 적용되지
 * 않으므로 별도 빈으로 분리). 충돌 시 본 트랜잭션만 롤백되고 호출 루프는 다음 행을 계속 처리한다.
 */
@Service
class ProductBarcodeRowUpsertService(
    private val productBarcodeRepository: ProductBarcodeRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun persistRow(
        command: ProductBarcodeUpsertCommand,
        key: String,
        matchedProduct: Product?,
        productCode: String?,
        productUnit: String?,
        barcode: String?
    ) {
        val existing = productBarcodeRepository.findByCustomKey(key)
        val entity = existing ?: ProductBarcode(customKey = key)
        applyToEntity(entity, command, key, matchedProduct, productCode, productUnit, barcode)
        productBarcodeRepository.saveAndFlush(entity)
    }

    private fun applyToEntity(
        entity: ProductBarcode,
        command: ProductBarcodeUpsertCommand,
        key: String,
        matchedProduct: Product?,
        productCode: String?,
        productUnit: String?,
        barcode: String?
    ) {
        entity.customKey = key
        entity.name = productUnit
        entity.unit = productUnit
        entity.barcode = barcode
        entity.sortOrder = command.productSequence
        entity.productName = command.productName
        // 레거시 ProductCode__c 평문 보존 — lookup 실패해도 코드 추적 가능.
        entity.productCode = productCode
        // 레거시 정합 — Product 미매칭이면 null (orphan 허용).
        entity.productId = matchedProduct?.id
    }
}

private fun Throwable.mostSpecificCauseText(): String {
    var cause: Throwable = this
    while (cause.cause != null && cause.cause !== cause) cause = cause.cause!!
    return cause.message ?: cause::class.simpleName ?: "unknown"
}
