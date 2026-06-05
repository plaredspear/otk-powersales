package com.otoki.powersales.order.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.draft.entity.TmpOrder
import com.otoki.powersales.draft.entity.TmpOrderProduct
import com.otoki.powersales.draft.repository.TmpOrderRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.request.OrderDraftRequest
import com.otoki.powersales.order.dto.response.OrderDraftDetailResponse
import com.otoki.powersales.order.dto.response.OrderDraftLineResponse
import com.otoki.powersales.order.dto.response.OrderDraftSaveResponse
import com.otoki.powersales.order.exception.OrderDraftAccountForbiddenException
import com.otoki.powersales.order.exception.OrderDraftInvalidRequestException
import com.otoki.powersales.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주문 임시저장 (Draft) 서비스 — Spec #596.
 *
 * - **사번당 1건 정책 (Q2)**: DB UNIQUE(`employee_id`) 강제. UPSERT 시 row lock 직렬화 (§2.5).
 * - **트랜잭션 보강 (Q9)**: 등록/삭제 모두 단일 `@Transactional`. 레거시 부분 적재 결함 보강.
 * - **납기일 미보관 (Q8)**: `tmp_order` 에 컬럼 부재. 사용자가 복원 시 폼에서 재입력.
 * - **정식 등록 후 자동 삭제 (Q4)**: `OrderRequestCreateService` 가 본 서비스의
 *   [deleteByEmployeeId] 를 호출 (#592 트랜잭션 내).
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

        if (account.employeeCode.isNullOrBlank() || account.employeeCode != employee.employeeCode) {
            throw OrderDraftAccountForbiddenException()
        }

        // §2.5 row lock — 동일 사번 동시 등록 직렬화. 기존 row 가 있으면 잠금, 없으면 첫 INSERT 가 UNIQUE 보장.
        val existing = tmpOrderRepository.findByEmployeeIdForUpdate(userId)
        if (existing != null) {
            tmpOrderRepository.delete(existing)
            tmpOrderRepository.flush()
        }

        val draft = TmpOrder(
            tmpEmployeeCode = employee.employeeCode,
            tmpAccountCode = account.externalKey,
            tmpOrderDate = LocalDate.now(),
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
        val productNames = productRepository.findByProductCodeIn(productCodes)
            .associate { (it.productCode ?: "") to it.name }
        val lines = draft.products
            .sortedBy { it.lineNumber ?: Int.MAX_VALUE }
            .map { line ->
                val name = line.tmpProductCode?.let { productNames[it] }
                OrderDraftLineResponse(
                    lineNumber = line.lineNumber ?: 0,
                    productCode = line.tmpProductCode.orEmpty(),
                    productName = name,
                    unit = line.unit ?: inferUnit(line),
                    quantity = line.quantity ?: java.math.BigDecimal.ZERO,
                    quantityPieces = line.quantityPieces,
                    quantityBoxes = line.quantityBoxes,
                    unitPrice = line.unitPrice,
                    amount = line.amount,
                )
            }
        return OrderDraftDetailResponse(
            draftId = draft.id,
            accountId = draft.accountId ?: 0,
            accountName = account?.name.orEmpty(),
            accountExternalKey = account?.externalKey,
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
