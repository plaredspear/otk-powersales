package com.otoki.powersales.order.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.exception.OrderAccountForbiddenException
import com.otoki.powersales.order.exception.OrderInvalidRequestException
import com.otoki.powersales.order.exception.OrderInvalidUnitException
import com.otoki.powersales.order.exception.OrderLoanExceededException
import com.otoki.powersales.order.exception.OrderProductRestrictedException
import com.otoki.powersales.order.repository.OrderRequestProductRepository
import com.otoki.powersales.order.repository.OrderRequestRepository
import com.otoki.powersales.order.sap.client.InventoryInfo
import com.otoki.powersales.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.order.sap.client.SapLoanInquiryClient
import com.otoki.powersales.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.order.util.UnitConverter
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 주문 등록 핵심 트랜잭션 서비스 (Spec #592).
 *
 * **흐름** (단일 DB 트랜잭션):
 *  1. 멱등 검사 — `clientRequestId` 가 전달되면 기존 row 조회 후 200 OK 멱등 반환 (SAP 호출 없음)
 *  2. 입력 검증 — 형식 + 본인 담당 거래처 / 미래 일자
 *  3. SAP `InventorySearch` 1회 호출 — 단위 환산/공급제한/제품마스터 메타 일괄 조회 (응답 라인 누락 시 거부)
 *  4. SAP `LoanInquiry` 호출 — 여신 한도 서버 재검증 (`creditBalance >= totalAmount`)
 *  5. `order_request` 헤더 INSERT — 백엔드 자체 채번 `ORD-YYYYMMDD-{seq}`, 초기 status `SENT`
 *  6. `order_request_product` 라인 일괄 INSERT — `pieces_per_box` 등록 시점 스냅샷 (#595 의존)
 *  7. `sap_outbox` 행 INSERT — `domain_type='ORDER_REQUEST_REGISTER'`, status `PENDING`
 *
 * 워커는 별도 트랜잭션에서 폴링/송신/도메인 상태 갱신.
 */
@Service
@Transactional(readOnly = true)
class OrderRequestCreateService(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val inventorySearchClient: SapInventorySearchClient,
    private val loanInquiryClient: SapLoanInquiryClient,
    private val orderRequestRegisterSender: OrderRequestRegisterSender,
    private val entityManager: EntityManager,
) {

    @Transactional
    fun create(userId: Long, request: OrderRequestCreateRequest): OrderRequestCreateResponse {
        // 1. 멱등 검사
        if (!request.clientRequestId.isNullOrBlank()) {
            val existing = orderRequestRepository.findByClientRequestId(request.clientRequestId)
            if (existing != null) {
                return OrderRequestCreateResponse.from(existing)
            }
        }

        // 2. 입력 검증
        validateRequest(request)
        val employee = employeeRepository.findById(userId)
            .orElseThrow { OrderAccountForbiddenException() }
        val account = accountRepository.findById(request.accountId.toInt())
            .orElseThrow { OrderInvalidRequestException("거래처를 찾을 수 없습니다") }

        if (account.employeeCode.isNullOrBlank() || account.employeeCode != employee.employeeCode) {
            throw OrderAccountForbiddenException()
        }

        // 3. SAP InventorySearch
        val productCodes = request.lines.map { it.productCode }.distinct()
        val inventoryMap = inventorySearchClient.search(request.accountId, productCodes)
        validateInventory(request, inventoryMap)

        // 4. 여신 검증
        val totalAmountBD = BigDecimal.valueOf(request.totalAmount)
        val creditBalance = loanInquiryClient.inquireCreditBalance(request.accountId)
        if (creditBalance < totalAmountBD) {
            throw OrderLoanExceededException(creditBalance, totalAmountBD)
        }

        // 5. order_request 헤더 INSERT
        val now = LocalDateTime.now()
        val orderRequestNumber = nextOrderRequestNumber(now.toLocalDate())
        val header = OrderRequest(
            orderRequestNumber = orderRequestNumber,
            clientRequestId = request.clientRequestId,
            employeeSfid = employee.sfid,
            accountSfid = account.sfid,
            orderDate = now,
            deliveryDate = request.deliveryDate,
            totalAmount = totalAmountBD,
            totalApprovedAmount = BigDecimal.ZERO,
            orderRequestStatus = OrderRequestStatus.SENT,
            isClosed = false,
            employee = employee,
            account = account,
        )
        val savedHeader = orderRequestRepository.save(header)

        // 6. 라인 일괄 INSERT
        val savedLines = request.lines.map { line ->
            val info = inventoryMap.getValue(line.productCode)
            OrderRequestProduct(
                lineNumber = line.lineNumber,
                productCode = line.productCode,
                productName = info.productName,
                quantityBoxes = line.quantityBoxes,
                quantityPieces = line.quantityPieces,
                unit = line.unit,
                unitPrice = info.unitPrice,
                amount = info.unitPrice.multiply(BigDecimal.valueOf(line.quantityPieces.toLong())),
                piecesPerBox = info.conversionQuantity.coerceAtLeast(1),
                minOrderUnit = 1,
                supplyQuantity = info.supplyLimitQuantity,
                dcQuantity = 0,
                isCancelled = false,
                orderRequest = savedHeader,
            )
        }
        orderRequestProductRepository.saveAll(savedLines)

        // 7. sap_outbox 적재
        orderRequestRegisterSender.enqueue(savedHeader, savedLines)

        return OrderRequestCreateResponse.from(savedHeader)
    }

    private fun validateRequest(request: OrderRequestCreateRequest) {
        if (request.deliveryDate.isBefore(LocalDate.now())) {
            throw OrderInvalidRequestException("납기일은 오늘 이후여야 합니다")
        }
        val lineNumbers = request.lines.map { it.lineNumber }
        if (lineNumbers.distinct().size != lineNumbers.size) {
            throw OrderInvalidRequestException("동일 요청 내 lineNumber 가 중복되었습니다")
        }
        request.lines.forEach { line ->
            if (line.unit !in ALLOWED_UNITS) {
                throw OrderInvalidUnitException("unit 은 ${ALLOWED_UNITS} 중 하나여야 합니다 (productCode: ${line.productCode})")
            }
        }
    }

    private fun validateInventory(
        request: OrderRequestCreateRequest,
        inventoryMap: Map<String, InventoryInfo>,
    ) {
        request.lines.forEach { line ->
            val info = inventoryMap[line.productCode]
                ?: throw OrderInvalidRequestException("제품 마스터 미등록 (productCode: ${line.productCode})")

            // 단위 환산 정합 검증
            val piecesValid = UnitConverter.isPiecesValid(
                quantity = line.quantity,
                unit = line.unit,
                quantityPieces = line.quantityPieces,
                conversionQuantity = info.conversionQuantity,
            )
            if (!piecesValid) {
                throw OrderInvalidUnitException(
                    "단위 환산 정합 위반 (productCode: ${line.productCode}, unit: ${line.unit}, " +
                        "quantity: ${line.quantity}, quantityPieces: ${line.quantityPieces}, " +
                        "conversionQuantity: ${info.conversionQuantity})"
                )
            }

            // 공급제한 검증
            if (info.supplyLimitQuantity < line.quantityPieces) {
                throw OrderProductRestrictedException(
                    productCode = line.productCode,
                    limit = info.supplyLimitQuantity,
                    requested = line.quantityPieces,
                )
            }
        }
    }

    private fun nextOrderRequestNumber(today: LocalDate): String {
        val seq = entityManager
            .createNativeQuery("SELECT nextval('powersales.order_request_number_seq')")
            .singleResult as Number
        return "ORD-${today.format(YYYYMMDD)}-${seq.toLong()}"
    }

    companion object {
        private val ALLOWED_UNITS = setOf("BOX", "EA")
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
