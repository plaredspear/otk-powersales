package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.request.OrderDraftRequest
import com.otoki.powersales.domain.activity.order.dto.response.OrderDraftDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderDraftLineResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderDraftSaveResponse
import com.otoki.powersales.domain.activity.order.exception.OrderDraftAccountForbiddenException
import com.otoki.powersales.domain.activity.order.exception.OrderDraftInvalidRequestException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.draft.entity.TmpOrder
import com.otoki.powersales.domain.activity.draft.entity.TmpOrderProduct
import com.otoki.powersales.domain.activity.draft.repository.TmpOrderRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 임시저장 (Draft) 서비스 — Spec #596.
 *
 * - **사번당 1건 정책 (Q2)**: DB UNIQUE(`employee_id`) 강제. UPSERT 시 row lock 직렬화 (§2.5).
 * - **트랜잭션 보강 (Q9)**: 등록/삭제 모두 단일 `@Transactional`. 레거시 부분 적재 결함 보강.
 * - **납기일 보관 (레거시 정합)**: 레거시 Heroku `saveTemp` 가 화면 `#DeliveryRequestDate` 를
 *   `tmp_orderdate` 로 저장·복원했던 것과 동일 — `tmp_order.order_date`(`tmpOrderDate`) 에 담는다.
 * - **SAP 등록 최종 성공(APPROVED) 시 임시저장 삭제**: 접수(SENT) 시점이 아니라 비동기 SAP 송신이
 *   최종 성공한 시점에만 삭제한다. `OrderRequestCreateService`(접수) 는 [deleteByEmployeeId] 를
 *   호출하지 않고, [com.otoki.powersales.domain.activity.order.sap.handler.OrderRequestSapOutboxStatusHandler]
 *   가 APPROVED 전이 시 호출한다. 이로써 `SEND_FAILED`(확정 거부/재시도 소진) 시엔 draft 가 보존되어
 *   "다시 재주문" 시 복원 가능하다. 사용자 명시적 DELETE(OrderDraftController)에서도 호출된다.
 *   (과거 Spec #596 Q4 자동 삭제는 현업 요청 2026-07-10 으로 철회됐다가, 삭제 시점을 SAP 최종 성공으로
 *   늦춰 재도입 — SEND_FAILED 재입력 부담을 없애는 조건을 충족하므로 복원.)
 */
@Service
@Transactional(readOnly = true)
class OrderDraftService(
    private val tmpOrderRepository: TmpOrderRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: ProductRepository,
) {

    private val allowedUnits = setOf("BOX", "EA")

    /**
     * 임시저장 등록 — UPSERT.
     *
     * §2.5 동시성 정책: pessimistic row lock 으로 직렬화. UNIQUE 충돌 발생 안 함.
     */
    @Transactional
    fun save(userId: Long, request: OrderDraftRequest): OrderDraftSaveResponse {
        validateRequest(request)

        val employee = employeeRepository.findById(userId)
            .orElseThrow { OrderDraftAccountForbiddenException() }
        val account = accountRepository.findById(request.accountId)
            .orElseThrow { OrderDraftInvalidRequestException("거래처를 찾을 수 없습니다") }

        // 거래처 담당(owner) 재검증은 하지 않는다 (레거시 정합 — 주문 등록과 동일 정책).
        // 거래처 후보는 일정 기반 셀렉터(`/accounts/my` scope=order)로만 결정되며,
        // account.employeeCode(거래처 마스터 담당사원) 게이트는 그 조회 집합과 어긋나
        // 일정만 잡힌(담당자가 다른) 거래처의 임시저장을 잘못 차단했다. → 제거.

        // §2.5 row lock — 동일 사번 동시 등록 직렬화. 기존 row 가 있으면 잠금, 없으면 첫 INSERT 가 UNIQUE 보장.
        val existing = tmpOrderRepository.findByEmployeeIdForUpdate(userId)
        if (existing != null) {
            tmpOrderRepository.delete(existing)
            tmpOrderRepository.flush()
        }

        val draft = TmpOrder(
            tmpEmployeeCode = employee.employeeCode,
            tmpAccountCode = account.externalKey,
            // 레거시 정합: tmp_orderdate 컬럼에 실제로는 납기일(DeliveryRequestDate)을 저장했다.
            tmpOrderDate = request.deliveryDate,
            tmpTotalAmount = request.totalAmount.toString(),
            accountId = request.accountId,
            employeeId = userId,
        )

        // 제품 마스터 일괄 조회 — productId 채움 (Heroku 호환). 미존재 시 null 허용 (검증은 정식 등록 시).
        val productIds = productRepository.findByProductCodeIn(request.lines.map { it.productCode })
            .associateBy({ it.productCode }, { it.id })
        request.lines.forEach { line ->
            val productId = productIds[line.productCode]
            val product = TmpOrderProduct(
                tmpEmployeeCode = employee.employeeCode,
                tmpProductCode = line.productCode,
                tmpBoxCnt = line.quantityBoxes?.toPlainString(),
                tmpEaCnt = line.quantityPieces?.toString(),
                tmpTotalCnt = line.quantity.toPlainString(),
                employeeId = userId,
                productId = productId,
                tmpOrder = draft,
                lineNumber = line.lineNumber,
                unit = line.unit,
                quantity = line.quantity,
                quantityPieces = line.quantityPieces,
                quantityBoxes = line.quantityBoxes,
                unitPrice = line.unitPrice,
                amount = line.amount,
            )
            draft.products += product
        }

        val saved = tmpOrderRepository.save(draft)
        return OrderDraftSaveResponse(draftId = saved.id, savedAt = LocalDateTime.now())
    }

    /**
     * 임시저장 조회 — 본인 사번 기준 단건 (없으면 null).
     */
    fun findByUserId(userId: Long): OrderDraftDetailResponse? {
        val draft = tmpOrderRepository.findByEmployeeId(userId) ?: return null
        val account = draft.accountId?.let { accountRepository.findById(it).orElse(null) }
        val productCodes = draft.products.mapNotNull { it.tmpProductCode }.distinct()
        // 레거시 selectTempPrdList 정합: 복원 라인의 단가·입수는 저장된 temp 값이 아니라
        // 제품 마스터(JOIN dkretail__product__c)에서 재조회한다.
        val productsByCode = productRepository.findByProductCodeIn(productCodes)
            .associateBy { it.productCode ?: "" }
        val lines = draft.products
            .sortedBy { it.lineNumber ?: Int.MAX_VALUE }
            .map { line ->
                val product = line.tmpProductCode?.let { productsByCode[it] }
                // 단가 = 표준단가 + 주세 (레거시 `(standardunitprice__c + supertax__c)`).
                // 제품 마스터에 없으면(단종/삭제) 저장된 단가로 graceful fallback.
                val masterUnitPrice = product?.let {
                    (it.standardUnitPrice ?: BigDecimal.ZERO) + (it.superTax ?: BigDecimal.ZERO)
                }
                OrderDraftLineResponse(
                    lineNumber = line.lineNumber ?: 0,
                    productCode = line.tmpProductCode.orEmpty(),
                    productName = product?.name,
                    unit = line.unit ?: inferUnit(line),
                    quantity = line.quantity ?: BigDecimal.ZERO,
                    quantityPieces = line.quantityPieces,
                    quantityBoxes = line.quantityBoxes,
                    boxSize = product?.boxReceivingQuantity?.toInt() ?: 0,
                    unitPrice = masterUnitPrice ?: line.unitPrice,
                    amount = line.amount,
                )
            }
        return OrderDraftDetailResponse(
            draftId = draft.id,
            accountId = draft.accountId ?: 0,
            accountName = account?.name.orEmpty(),
            accountExternalKey = account?.externalKey,
            deliveryDate = draft.tmpOrderDate,
            totalAmount = draft.tmpTotalAmount?.toLongOrNull() ?: 0L,
            savedAt = draft.updatedAt,
            lines = lines,
        )
    }

    /**
     * 임시저장 삭제 — 본인 사번 기준. 없어도 멱등 (예외 없음).
     */
    @Transactional
    fun deleteByEmployeeId(employeeId: Long) {
        tmpOrderRepository.deleteByEmployeeId(employeeId)
    }

    private fun validateRequest(request: OrderDraftRequest) {
        val lineNumbers = request.lines.map { it.lineNumber }
        if (lineNumbers.distinct().size != lineNumbers.size) {
            throw OrderDraftInvalidRequestException("동일 요청 내 lineNumber 가 중복되었습니다")
        }
        request.lines.forEach { line ->
            if (line.unit !in allowedUnits) {
                throw OrderDraftInvalidRequestException(
                    "unit 은 ${allowedUnits} 중 하나여야 합니다 (productCode: ${line.productCode})",
                )
            }
        }
    }

    /**
     * 신규 컬럼이 비어 있는 레거시(Heroku) row 의 단위 추정 (`box_cnt` ≠ 0 → BOX, 그 외 EA).
     * 본 스펙 신규 등록은 항상 `unit` 을 채우므로 이 경로는 마이그레이션 잔존 데이터 호환용.
     */
    private fun inferUnit(line: TmpOrderProduct): String {
        val boxNonZero = line.tmpBoxCnt?.toBigDecimalOrNull()?.signum() ?: 0
        return if (boxNonZero > 0) "BOX" else "EA"
    }
}
