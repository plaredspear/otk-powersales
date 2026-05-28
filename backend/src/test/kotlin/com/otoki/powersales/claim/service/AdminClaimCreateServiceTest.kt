package com.otoki.powersales.claim.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.claim.enums.ClaimChannel
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.claim.exception.InvalidClaimDateException
import com.otoki.powersales.claim.exception.ReceiptRequiredException
import com.otoki.powersales.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.promotion.exception.AccountNotFoundException
import com.otoki.powersales.sf.outbound.SfApiResponse
import com.otoki.powersales.sf.outbound.SfOutboundClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@DisplayName("AdminClaimCreateService 테스트")
class AdminClaimCreateServiceTest {

    private lateinit var claimRepository: ClaimRepository
    private lateinit var uploadFileRepository: UploadFileRepository
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var fileStorageService: FileStorageService
    private lateinit var storageService: StorageService
    private lateinit var sfOutboundClient: SfOutboundClient
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
        storageService = mockk()
        sfOutboundClient = mockk()
        txTemplate = mockk()
        every { txTemplate.execute<Any>(any()) } answers {
            val callback = arg<TransactionCallback<Any>>(0)
            callback.doInTransaction(mockk(relaxed = true))
        }
        service = AdminClaimCreateService(
            claimRepository,
            uploadFileRepository,
            employeeRepository,
            accountRepository,
            productRepository,
            fileStorageService,
            storageService,
            sfOutboundClient,
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
            // id 를 시뮬레이션
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
        every { claimRepository.findById(idAfterSave) } answers {
            Optional.of(
                Claim(
                    id = idAfterSave,
                    employee = newEmployee(),
                    account = newAccount(),
                    product = newProduct(),
                    date = LocalDate.now(),
                    claimType1 = ClaimType1.A,
                    claimType2 = ClaimType2.AA,
                    defectDescription = "이물질 발견",
                    defectQuantity = BigDecimal("5"),
                    status = ClaimStatus.SF_PENDING,
                    channel = ClaimChannel.WEB,
                )
            )
        }
    }

    @Test
    @DisplayName("정상 등록 (SF 200 응답) → status=SENT")
    fun create_sfSuccess() {
        stubLookups()
        stubClaimSave()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = "{}")

        val response = service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(response.status).isEqualTo("SENT")
        assertThat(response.sfResultCode).isEqualTo("200")
        assertThat(apiMapSlot.captured["Channel"]).isEqualTo("WEB")
        assertThat(apiMapSlot.captured["SAPAccountCode"]).isEqualTo("SAP-001")
        assertThat(apiMapSlot.captured["EmployeeCode"]).isEqualTo("EMP-001")
        assertThat(apiMapSlot.captured["ExpirationDate"]).isEqualTo("20270101")
        assertThat(apiMapSlot.captured["ManufacturingDate"]).isEqualTo("")
        assertThat(apiMapSlot.captured["Quantity"]).isEqualTo("5")
    }

    @Test
    @DisplayName("UploadFile 저장 — uploadKbn = claim/part/receipt (SF customName__c prefix 정합)")
    fun create_uploadKbnMatchesSfPrefix() {
        stubLookups()
        stubClaimSave()
        val savedSlot = slot<List<UploadFile>>()
        every { uploadFileRepository.saveAll(capture(savedSlot)) } answers { firstArg() }
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = "{}")

        service.createClaim(
            request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = newPhoto("receipt"),
        )

        val photos = savedSlot.captured
        assertThat(photos).hasSize(3)
        assertThat(photos.map { it.uploadKbn }).containsExactly("claim", "part", "receipt")
        assertThat(photos.all { it.parentType == "DKRetail__Claim__c" }).isTrue
    }

    @Test
    @DisplayName("SF 응답 RESULT_CODE=0 → status=SEND_FAILED + send_fail_message 박제")
    fun create_sfFailure() {
        stubLookups()
        stubClaimSave()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "0", resultMsg = "DML 에러", rawBody = "{}")

        val response = service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(response.status).isEqualTo("SEND_FAILED")
        assertThat(response.sfResultCode).isEqualTo("0")
        assertThat(response.sfResultMsg).isEqualTo("DML 에러")
    }

    @Test
    @DisplayName("SF HTTP 예외 → status=SEND_FAILED + sfResultCode=null")
    fun create_sfException() {
        stubLookups()
        stubClaimSave()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } throws RuntimeException("timeout")

        val response = service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(response.status).isEqualTo("SEND_FAILED")
        assertThat(response.sfResultCode).isNull()
        assertThat(response.sfResultMsg).contains("timeout")
    }

    @Test
    @DisplayName("Account 미존재 → AccountNotFoundException 422, SF 호출 X")
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

        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
        verify(exactly = 0) { claimRepository.save(any()) }
    }

    @Test
    @DisplayName("영수증 누락 (purchaseMethod=B) → ReceiptRequiredException 422, SF 호출 X")
    fun create_receiptRequired() {
        assertThatThrownBy {
            service.createClaim(
                request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(ReceiptRequiredException::class.java)

        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }

    @Test
    @DisplayName("영수증 포함 (purchaseMethod=B + receipt 첨부) → 정상")
    fun create_withReceipt() {
        stubLookups()
        stubClaimSave()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = "{}")

        val response = service.createClaim(
            request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = newPhoto("receipt"),
        )

        assertThat(response.status).isEqualTo("SENT")
        assertThat(apiMapSlot.captured["PurchaseMethod"]).isEqualTo("B")
        assertThat(apiMapSlot.captured["Amount"]).isEqualTo("10000")
        assertThat(apiMapSlot.captured["ReceiptImageBuffer"]).isNotNull()
        assertThat(apiMapSlot.captured["ReceiptImageBuffer"].toString()).isNotEmpty()
    }

    @Test
    @DisplayName("requestType 5개 → RequestTypeMaxExceededException 422, SF 호출 X")
    fun create_requestTypeMaxExceeded() {
        assertThatThrownBy {
            service.createClaim(
                request = newRequest(requestType = "의견서;상담;긴급처리(FS사업부);판매취소 필요;물량수거 요청"),
                claimPhoto = newPhoto("claim"),
                partPhoto = newPhoto("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(RequestTypeMaxExceededException::class.java)

        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }

    @Test
    @DisplayName("미래 제조일자 → InvalidClaimDateException 400, SF 호출 X")
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

        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }

    @Test
    @DisplayName("dateType=MANUFACTURE_DATE — apiMap.ManufacturingDate 만 채워지고 ExpirationDate=\"\"")
    fun create_manufactureDateBranch() {
        stubLookups()
        stubClaimSave()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = "{}")

        val req = newRequest().copy(
            dateType = "MANUFACTURE_DATE",
            expirationDate = null,
            manufacturingDate = "2025-01-01",
        )
        service.createClaim(
            request = req,
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(apiMapSlot.captured["ManufacturingDate"]).isEqualTo("20250101")
        assertThat(apiMapSlot.captured["ExpirationDate"]).isEqualTo("")
    }

    @Test
    @DisplayName("send_attempt_count — 신규 등록 후 1 (성공/실패 무관 +1)")
    fun create_sendAttemptCountIncrement() {
        stubLookups()
        val foundClaim = Claim(
            id = 42L,
            employee = newEmployee(),
            account = newAccount(),
            product = newProduct(),
            date = LocalDate.now(),
            claimType1 = ClaimType1.A,
            claimType2 = ClaimType2.AA,
            defectDescription = "이물질",
            defectQuantity = BigDecimal("5"),
            status = ClaimStatus.SF_PENDING,
            channel = ClaimChannel.WEB,
        )
        every { claimRepository.save(any()) } returns foundClaim
        every { claimRepository.findById(42L) } returns Optional.of(foundClaim)
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = "{}")

        service.createClaim(
            request = newRequest(),
            claimPhoto = newPhoto("claim"),
            partPhoto = newPhoto("part"),
            receiptPhoto = null,
        )

        assertThat(foundClaim.sendAttemptCount).isEqualTo(1)
        assertThat(foundClaim.status).isEqualTo(ClaimStatus.SENT)
        assertThat(foundClaim.sentAt).isNotNull()
    }
}
