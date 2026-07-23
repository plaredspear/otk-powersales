package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.event.OrderRequestRegisteredEvent
import com.otoki.powersales.domain.activity.order.exception.OrderAccountForbiddenException
import com.otoki.powersales.domain.activity.order.exception.OrderDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderInvalidRequestException
import com.otoki.powersales.domain.activity.order.exception.OrderInvalidUnitException
import com.otoki.powersales.domain.activity.order.exception.OrderLoanExceededException
import com.otoki.powersales.domain.activity.order.exception.OrderProductRestrictedException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.client.InventoryInfo
import com.otoki.powersales.domain.activity.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.domain.activity.order.sap.client.SapLoanInquiryClient
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.domain.activity.order.util.UnitConverter
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.enums.ProductType
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주문 등록 핵심 트랜잭션 서비스 (Spec #592).
 *
 * **흐름** (단일 DB 트랜잭션):
 *  1. 멱등 검사 — `clientRequestId` 가 전달되면 기존 row 조회 후 200 OK 멱등 반환 (SAP 호출 없음)
 *  2. 입력 검증 — 형식 / 미래 일자 (거래처 담당 재검증 없음 — 레거시 정합, 일정 기반 셀렉터만 게이트)
 *  3. 제품 마스터 대조 — 클라이언트 제공 productCode 가 마스터에 없으면 SAP 호출 전 즉시 거부
 *  3-1. 전용상품 차단 — product_type='2' 라인 거부 (20010042 레거시 예외 허용)
 *  3-2. SAP `InventorySearch` 호출 — 단위 환산/공급제한/제품마스터 메타 일괄 조회 (응답 라인 누락 시 거부)
 *  4. SAP `LoanInquiry` 호출 — 여신 한도 서버 재검증 (`creditBalance >= totalAmount`)
 *  5. `order_request` 헤더 INSERT — 백엔드 자체 채번 `OR{00000000}` (레거시 SF Auto Number 동폭), 초기 status `SENT`
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
    private val productRepository: ProductRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val inventorySearchClient: SapInventorySearchClient,
    private val loanInquiryClient: SapLoanInquiryClient,
    private val orderRequestRegisterSender: OrderRequestRegisterSender,
    private val orderDeadlineCalculator: OrderDeadlineCalculator,
    private val entityManager: EntityManager,
    private val eventPublisher: ApplicationEventPublisher,
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
        val account = accountRepository.findById(request.accountId)
            .orElseThrow { OrderInvalidRequestException("거래처를 찾을 수 없습니다") }

        // 거래처 담당(owner) 재검증은 하지 않는다 (레거시 정합).
        // 레거시 주문 저장(reqOrder)은 화면이 보낸 거래처 코드를 무검증 신뢰했고, 거래처 후보는
        // 전적으로 일정(방문/진열) 기반 셀렉터(`/accounts/my` scope=order)로만 결정된다.
        // account.employeeCode(거래처 마스터 담당사원) 기준 게이트는 그 일정 기반 조회 집합과
        // 어긋나, 일정만 잡힌(담당자가 다른) 거래처 주문을 잘못 차단했다. → 제거.

        // 3. 제품 마스터 대조 가드 — SAP InventorySearch 호출 전, 클라이언트(제품검색/임시저장 복원)가
        //    보낸 productCode 를 제품 마스터와 대조해 미존재 코드는 즉시 거부한다. 비정상 코드가
        //    SAP SD03070 ABAP 숫자 변환에서 시스템 오류(HTTP 500)로 터진 운영 사건의 재발 방어.
        //    (레거시도 주문 제품 후보가 마스터 조회 결과로만 구성되어 미존재 코드는 도달 불가 — 동작 동등)
        val productCodes = request.lines.map { it.productCode }.distinct()
        val productsByCode = productRepository.findByProductCodeIn(productCodes)
            .associateBy { it.productCode }
        val unknownCodes = productCodes.filterNot(productsByCode::containsKey)
        if (unknownCodes.isNotEmpty()) {
            throw OrderInvalidRequestException(
                "제품 마스터에 없는 제품코드입니다: ${unknownCodes.joinToString(", ")}"
            )
        }

        // 3-1. 전용상품 차단 — 레거시 주문 화면(poplayer.js prdType=='2' 차단) 정합 서버 가드.
        //      화면 차단을 우회하는 경로(임시저장 복원, 구버전 앱 등) 방어 + SAP 의 불명확한
        //      거부 메시지("유효한 데이터가 아닙니다") 대신 명확한 사유를 반환한다.
        //      20010042(옛날_구수한끓여먹는누룽지 450g)는 레거시 하드코딩 예외 동일 적용.
        val exclusiveCodes = productCodes.filter { code ->
            code != EXCLUSIVE_BLOCK_EXEMPT_CODE &&
                productsByCode.getValue(code).productType == ProductType.PRODUCT_TYPE_2
        }
        if (exclusiveCodes.isNotEmpty()) {
            throw OrderInvalidRequestException(
                "전용상품은 주문할 수 없습니다: ${exclusiveCodes.joinToString(", ")}"
            )
        }

        // 3-2. SAP InventorySearch
        val inventoryMap = inventorySearchClient.search(request.accountId, productCodes, request.deliveryDate)
        validateInventory(request, inventoryMap)

        // 4. 여신 검증
        val totalAmountBD = BigDecimal.valueOf(request.totalAmount)
        val creditBalance = loanInquiryClient.inquireCreditBalance(request.accountId)
        if (creditBalance < totalAmountBD) {
            throw OrderLoanExceededException(creditBalance, totalAmountBD)
        }

        // 5. order_request 헤더 INSERT
        val now = LocalDateTime.now()
        val orderRequestNumber = nextOrderRequestNumber()
        val header = OrderRequest(
            orderRequestNumber = orderRequestNumber,
            clientRequestId = request.clientRequestId,
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
        // product FK(product_id) 채움 — step 3 제품 마스터 대조 결과 재사용 (레거시 ProductCode 로
        // DKRetail__Product__c 조회해 DKRetail__ProductId__c set 한 동등 처리).
        val savedLines = request.lines.map { line ->
            val info = inventoryMap.getValue(line.productCode)
            // 레거시 정합: 박스 수량은 총 EA ÷ 환산수량으로 서버가 역산(클라이언트 박스 입력값 비신뢰).
            // 박스+낱개 혼합도 총 EA 가 환산수량 배수면 박스 수로 흡수됨 (예: 박스5+낱개8, 환산8 → 6박스).
            val derivedBoxes = UnitConverter.toBoxQuantity(line.quantityPieces, info.conversionQuantity)
            OrderRequestProduct(
                lineNumber = BigDecimal.valueOf(line.lineNumber.toLong()),
                productCode = line.productCode,
                quantityBoxes = derivedBoxes,
                quantityPieces = BigDecimal.valueOf(line.quantityPieces.toLong()),
                // 레거시 정합: 저장/SAP 송신 단위는 SAP MinOrderingUnit (OrderController.java:664 setUnit(minOrderingUnit)).
                // 클라이언트 unit 은 무시. 공란이면 빈 문자열 그대로 (레거시 setUnit("") 동등).
                unit = info.minOrderingUnit,
                unitPrice = info.unitPrice,
                amount = info.unitPrice.multiply(BigDecimal.valueOf(line.quantityPieces.toLong())),
                piecesPerBox = info.conversionQuantity.coerceAtLeast(1),
                minOrderUnit = 1,
                supplyQuantity = info.supplyLimitQuantity,
                dcQuantity = 0,
                orderRequest = savedHeader,
                product = productsByCode[line.productCode],
            )
        }
        orderRequestProductRepository.saveAll(savedLines)

        // 7. sap_outbox 적재
        val outbox = orderRequestRegisterSender.enqueue(savedHeader, savedLines)

        // 7-1. 커밋 후 SAP SD03050 송신 트리거 (비동기). 스케줄러(SapOutboxBatch) 비활성 상태에서도
        //      주문 등록 즉시 SD03050 이 호출되도록 한다. 실제 송신/상태갱신/재시도는
        //      OrderRequestRegisterDispatcher → SapOutboxBatchService.processOne 가 수행.
        eventPublisher.publishEvent(OrderRequestRegisteredEvent(outbox.id))

        // 임시저장(tmp_order) 삭제는 접수(SENT) 시점이 아니라 비동기 SAP 등록이 최종 성공(APPROVED)한
        // 시점에 OrderRequestSapOutboxStatusHandler 가 수행한다. 여기(접수)서는 삭제하지 않는다 — SAP 가
        // 확정 거부/재시도 소진으로 SEND_FAILED 가 되면 draft 를 보존해 "다시 재주문" 시 복원할 수 있게 하기 위함.

        return OrderRequestCreateResponse.from(savedHeader)
    }

    private fun validateRequest(request: OrderRequestCreateRequest) {
        if (request.deliveryDate.isBefore(LocalDate.now())) {
            throw OrderInvalidRequestException("납기일은 오늘 이후여야 합니다")
        }
        // server-side 마감 가드 (레거시 reqOrder dateConfirm 동등) — 모바일 검증 우회 직접 호출 차단.
        if (!orderDeadlineCalculator.isWithinDeadline(request.deliveryDate)) {
            throw OrderDeadlinePassedException()
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
            val conv = info.conversionQuantity.coerceAtLeast(1)
            // 레거시 정합: 단위는 클라이언트 unit 이 아니라 SAP MinOrderingUnit 으로 결정 (OrderController.java:548,664).
            val unit = info.minOrderingUnit

            // 단위 환산 정합 검증 — 레거시 OrderController.java:630-632.
            // 박스류(EA 외)는 총 EA(quantityPieces)가 환산수량 배수여야 정합 (박스+낱개 혼합은 총 EA 로 평탄화).
            if (!UnitConverter.isPiecesValid(unit, line.quantityPieces, conv)) {
                throw OrderInvalidUnitException(
                    "단위 환산 정합 위반 (productCode: ${line.productCode}, unit: $unit, " +
                            "quantityPieces: ${line.quantityPieces}, conversionQuantity: $conv)"
                )
            }

            // 공급제한 검증 — 레거시 OrderController.java:557-558,650.
            // SAP SupplyLimitQTY 는 주문 단위(박스) 기준이므로 EA 외 단위는 환산수량을 곱해 총 EA 와 비교.
            // (Int.MAX_VALUE = 제한 없음 → ×conv 오버플로 방지를 위해 Long 연산.)
            val supplyLimitPieces: Long =
                if (unit == UNIT_EA) info.supplyLimitQuantity.toLong()
                else info.supplyLimitQuantity.toLong() * conv.toLong()
            if (supplyLimitPieces < line.quantityPieces.toLong()) {
                throw OrderProductRestrictedException(
                    productCode = line.productCode,
                    limit = info.supplyLimitQuantity,
                    requested = line.quantityPieces,
                )
            }
        }
    }

    // 레거시 SF Auto Number `OP{00000000}` 와 동일 폭 (prefix 2자 + 8자리) 으로 채번.
    // prefix 는 SF 사용분(OP/OG/EP …)과 충돌하지 않는 OR 로 분리 — 마이그레이션 데이터와 번호공간 미겹침.
    private fun nextOrderRequestNumber(): String {
        val seq = entityManager
            .createNativeQuery("SELECT nextval('powersales.order_request_number_seq')")
            .singleResult as Number
        return "$ORDER_REQUEST_NUMBER_PREFIX${seq.toLong().toString().padStart(ORDER_REQUEST_NUMBER_DIGITS, '0')}"
    }

    companion object {
        private const val UNIT_EA = "EA"
        private val ALLOWED_UNITS = setOf("BOX", "EA")

        /** 전용상품 차단 예외 제품코드 — 옛날_구수한끓여먹는누룽지 450g (레거시 poplayer.js 하드코딩 정합). */
        private const val EXCLUSIVE_BLOCK_EXEMPT_CODE = "20010042"
        private const val ORDER_REQUEST_NUMBER_PREFIX = "OR"
        private const val ORDER_REQUEST_NUMBER_DIGITS = 8
    }
}
