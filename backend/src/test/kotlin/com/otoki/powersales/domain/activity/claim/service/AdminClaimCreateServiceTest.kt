package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimDateException
import com.otoki.powersales.domain.activity.claim.exception.ReceiptRequiredException
import com.otoki.powersales.domain.activity.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.promotion.exception.AccountNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

/**
 * AdminClaimCreateService 테스트 — 입력 검증/조회/INSERT + SF 송신 이벤트 발행까지.
 *
 * SF push(/ClaimRegist 호출, apiMap 구성, status 전이)는 등록 트랜잭션 커밋 후 비동기로 분리됐다
 * ([ClaimSfPushDispatcher] → [ClaimSfDispatchService]). 해당 검증은 ClaimSfDispatchServiceTest 책임이며,
 * 본 테스트는 등록(SF_PENDING) 응답과 이벤트 발행만 다룬다.
 */
@DisplayName("AdminClaimCreateService 테스트")
class AdminClaimCreateServiceTest {

    private lateinit var claimRepository: ClaimRepository
    private lateinit var uploadFileRepository: UploadFileRepository
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var fileStorageService: FileStorageService
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var service: AdminClaimCreateService

    @BeforeEach
    fun setUp() {
        claimRepository = mockk()
        uploadFileRepository = mockk()
        employeeRepository = mockk()
        accountRepository = mockk()
        productRepository = mockk()
        fileStorageService = mockk()
        eventPublisher = mockk(relaxUnitFun = true)
        txTemplate = mockk()
        every { txTemplate.execute<Any>(any()) } answers {
            val callback = arg<TransactionCallback<Any>>(0)
            callback.doInTransaction(mockk(relaxed = true))
        }
        service = AdminClaimCreateService(
            employeeRepository,
            accountRepository,
            productRepository,
            fileStorageService,
            claimRepository,
            uploadFileRepository,
            eventPublisher,
            txTemplate,
        )

        every { fileStorageService.uploadClaimPhoto(any(), any(), any(), any()) } returnsMany listOf(
            "uploads/claim/2026/05/27/aaa.jpg",
            "uploads/claim/2026/05/27/bbb.jpg",
            "uploads/claim/2026/05/27/ccc.jpg",
        )
        every { uploadFileRepository.saveAll(any<List<UploadFile>>()) } answers { firstArg() }
    }

    private fun newRequest(
        purchaseMethod: String? = null,
        amount: BigDecimal? = null,
        requestType: String? = null,
    ) = AdminClaimCreateRequest(
        sapAccountCode = "SAP-001",
        productCode = "PROD-001",
        employeeCode = "EMP-001",
        dateType = "EXPIRY_DATE",
        expirationDate = "2027-01-01",
        manufacturingDate = null,
        claimDate = LocalDate.now().toString(),
        claimType1 = "A",
        claimType2 = "AA",
        quantity = BigDecimal("5"),
        description = "이물질 발견",
        purchaseMethod = purchaseMethod,
        amount = amount,
        requestType = requestType,
    )

    private fun newEmployee() = Employee(id = 1L, employeeCode = "EMP-001", name = "김영업").apply {
        costCenterCode = "CC-100"
        orgName = "영업1팀"
    }

    private fun newAccount() = Account(id = 1, name = "홍길동 슈퍼").apply {
        externalKey = "SAP-001"
        branchCode = "BR-100"
    }

    private fun newProduct() = Product(productCode = "PROD-001", name = "진라면 순한맛")

    private fun newPhoto(name: String) = MockMultipartFile(name, "$name.jpg", "image/jpeg", "binarydata".toByteArray())

    private fun stubLookups() {
        every { employeeRepository.findByEmployeeCode("EMP-001") } returns Optional.of(newEmployee())
        every { accountRepository.findByExternalKey("SAP-001") } returns newAccount()
        every { productRepository.findByProductCode("PROD-001") } returns newProduct()
    }

    private fun stubClaimSave(idAfterSave: Long = 42L) {
        val captured = slot<Claim>()
        every { claimRepository.save(capture(captured)) } answers {
            val src = captured.captured
            // id 를 시뮬레이션 (영속화된 PK 를 가진 entity 반환).
            Claim(
                id = idAfterSave,
                employee = src.employee,
                account = src.account,
                dateType = src.dateType,
                date = src.date,
                claimType1 = src.claimType1,
                claimType2 = src.claimType2,
                defectDescription = src.defectDescription,
                defectQuantity = src.defectQuantity,
                purchaseAmount = src.purchaseAmount,
                purchaseMethodCode = src.purchaseMethodCode,
                requestTypeCode = src.requestTypeCode,
                status = src.status,
                channel = src.channel,
                product = src.product,
                costCenterCode = src.costCenterCode,
                division = src.division,
            )
        }
    }

    @Test
    @DisplayName("정상 등록 → status=SF_PENDING + claimId 반환 + SF 송신 이벤트 발행 (channel=WEB INSERT)")
    fun create_success() {
        stubLookups()
        stubClaimSave()
        val claimSlot = slot<Claim>()
        every { claimRepository.save(capture(claimSlot)) } answers {
            Claim(id = 42L, status = claimSlot.captured.status, channel = claimSlot.captured.channel)
        }
        val eventSlot = slot<ClaimRegisteredEvent>()
        every { eventPublisher.publishEvent(capture(eventSlot)) } answers {}

        val response = service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        // 등록 직후 상태는 SF_PENDING (SF 송신은 커밋 후 비동기)
        assertThat(response.claimId).isEqualTo(42L)
        assertThat(response.status).isEqualTo("SF_PENDING")
        assertThat(response.sfResultCode).isNull()
        assertThat(response.sfResultMsg).isNull()
        // INSERT 시점 channel=WEB
        assertThat(claimSlot.captured.channel).isEqualTo(ClaimChannel.WEB)
        assertThat(claimSlot.captured.status).isEqualTo(ClaimStatus.SF_PENDING)
        // 커밋 후 SF 송신 트리거 이벤트가 claimId 로 발행됨
        verify { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
        assertThat(eventSlot.captured.claimId).isEqualTo(42L)
    }

    @Test
    @DisplayName("UploadFile 저장 — uploadKbn = claim/part/receipt (SF customName__c prefix 정합)")
    fun create_uploadKbnMatchesSfPrefix() {
        stubLookups()
        stubClaimSave()
        val savedSlot = slot<List<UploadFile>>()
        every { uploadFileRepository.saveAll(capture(savedSlot)) } answers { firstArg() }

        service.createClaim(
            request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = newPhoto("receipt"),
        )

        val photos = savedSlot.captured
        assertThat(photos).hasSize(3)
        assertThat(photos.map { it.uploadKbn }).containsExactly("claim", "part", "receipt")
        assertThat(photos.all { it.parentType == "Claim" }).isTrue
    }

    @Test
    @DisplayName("costCenterCode 는 거래처 branchCode 로 자동 채움 + division 공란 (SF 정합)")
    fun create_fillsCostCenterFromAccountBranch() {
        stubLookups()
        val claimSlot = slot<Claim>()
        every { claimRepository.save(capture(claimSlot)) } answers {
            Claim(id = 42L, status = claimSlot.captured.status, channel = claimSlot.captured.channel)
        }

        service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(claimSlot.captured.costCenterCode).isEqualTo("BR-100")
        assertThat(claimSlot.captured.division).isNull()
    }

    @Test
    @DisplayName("Account 미존재 → AccountNotFoundException 422, INSERT/이벤트 X")
    fun create_accountNotFound() {
        every { employeeRepository.findByEmployeeCode("EMP-001") } returns Optional.of(newEmployee())
        every { accountRepository.findByExternalKey("SAP-001") } returns null

        assertThatThrownBy {
            service.createClaim(
                request = newRequest(),
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(AccountNotFoundException::class.java)

        verify(exactly = 0) { claimRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
    }

    @Test
    @DisplayName("영수증 누락 (purchaseMethod=B) → ReceiptRequiredException 422, INSERT/이벤트 X")
    fun create_receiptRequired() {
        assertThatThrownBy {
            service.createClaim(
                request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(ReceiptRequiredException::class.java)

        verify(exactly = 0) { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
    }

    @Test
    @DisplayName("requestType 5개 → RequestTypeMaxExceededException 422, INSERT/이벤트 X")
    fun create_requestTypeMaxExceeded() {
        assertThatThrownBy {
            service.createClaim(
                request = newRequest(requestType = "의견서;상담;긴급처리(FS사업부);판매취소 필요;물량수거 요청"),
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(RequestTypeMaxExceededException::class.java)

        verify(exactly = 0) { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
    }

    @Test
    @DisplayName("미래 제조일자 → InvalidClaimDateException 400, INSERT/이벤트 X")
    fun create_futureManufacturingDate() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val req = newRequest().copy(
            dateType = "MANUFACTURE_DATE",
            expirationDate = null,
            manufacturingDate = tomorrow,
        )

        assertThatThrownBy {
            service.createClaim(
                request = req,
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(InvalidClaimDateException::class.java)
            .hasMessageContaining("제조일자")

        verify(exactly = 0) { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
    }
}
