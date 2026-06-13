package com.otoki.powersales.claim.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.claim.exception.ReceiptRequiredException
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
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
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@DisplayName("AdminClaimRegistTestService (SF 전송 테스트 도구) 테스트")
class AdminClaimRegistTestServiceTest {

    private lateinit var claimRepository: ClaimRepository
    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var createService: AdminClaimCreateService
    private lateinit var service: AdminClaimRegistTestService

    @BeforeEach
    fun setUp() {
        claimRepository = mockk(relaxed = true)
        sfOutboundClient = mockk()
        // createService 는 실제 인스턴스 — buildApiMapFromBytes / invokeSf 로직을 그대로 사용.
        // 테스트 도구는 DB 저장을 하지 않으므로 repository/storage 는 호출되지 않아야 한다 (relaxed mock).
        createService = AdminClaimCreateService(
            claimRepository = claimRepository,
            uploadFileRepository = mockk(relaxed = true),
            employeeRepository = mockk<EmployeeRepository>(relaxed = true),
            accountRepository = mockk<AccountRepository>(relaxed = true),
            productRepository = mockk<ProductRepository>(relaxed = true),
            fileStorageService = mockk<FileStorageService>(relaxed = true),
            storageService = mockk<StorageService>(relaxed = true),
            sfOutboundClient = sfOutboundClient,
            txTemplate = mockk<TransactionTemplate>(relaxed = true),
        )
        service = AdminClaimRegistTestService(createService, ObjectMapper())
    }

    private fun newRequest(
        purchaseMethod: String? = null,
        amount: BigDecimal? = null,
    ) = AdminClaimCreateRequest(
        sapAccountCode = "SAP-001",
        productCode = "PROD-001",
        employeeCode = "EMP-001",
        dateType = "EXPIRY_DATE",
        expirationDate = "2027-01-01",
        manufacturingDate = null,
        claimDate = "2026-06-11",
        claimType1 = "A",
        claimType2 = "AA",
        quantity = BigDecimal("5"),
        description = "이물질 발견",
        purchaseMethod = purchaseMethod,
        amount = amount,
        requestType = null,
    )

    private fun photo(name: String) =
        MockMultipartFile(name, "$name.jpg", "image/jpeg", "binarydata".toByteArray())

    private fun success() =
        SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = """{"RESULT_CODE":"200"}""")

    @Test
    @DisplayName("SF 로만 전송 — claimRepository.save 호출 안 함 (DB 미저장)")
    fun test_doesNotPersistToDb() {
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns success()

        val response = service.test(
            userId = 1L,
            request = newRequest(),
            claimPhoto = photo("claim"),
            partPhoto = photo("part"),
            receiptPhoto = null,
        )

        assertThat(response.success).isTrue()
        assertThat(response.resultCode).isEqualTo("200")
        verify(exactly = 1) { sfOutboundClient.callApi("/ClaimRegist", any()) }
        verify(exactly = 0) { claimRepository.save(any()) }
    }

    @Test
    @DisplayName("apiMap — 날짜 ISO(YYYY-MM-DD) + Amount 미입력 시 null")
    fun test_apiMapDateAndAmount() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns success()

        service.test(
            userId = 1L,
            request = newRequest(),
            claimPhoto = photo("claim"),
            partPhoto = photo("part"),
            receiptPhoto = null,
        )

        val sent = apiMapSlot.captured
        assertThat(sent["ExpirationDate"]).isEqualTo("2027-01-01")
        assertThat(sent["ClaimDate"]).isEqualTo("2026-06-11")
        assertThat(sent.containsKey("Amount")).isTrue()
        assertThat(sent["Amount"]).isNull()
    }

    @Test
    @DisplayName("이미지 미첨부 시 빈 Buffer 로 전송")
    fun test_noImageSendsEmptyBuffer() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns success()

        service.test(
            userId = 1L,
            request = newRequest(),
            claimPhoto = null,
            partPhoto = null,
            receiptPhoto = null,
        )

        assertThat(apiMapSlot.captured["ClaimImageBuffer"]).isEqualTo("")
        assertThat(apiMapSlot.captured["PartImageBuffer"]).isEqualTo("")
        assertThat(apiMapSlot.captured["ReceiptImageBuffer"]).isEqualTo("")
    }

    @Test
    @DisplayName("응답 payload 미리보기 — 이미지 Buffer 는 길이 표기로 마스킹")
    fun test_payloadMasksImageBuffer() {
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns success()

        val response = service.test(
            userId = 1L,
            request = newRequest(),
            claimPhoto = photo("claim"),
            partPhoto = photo("part"),
            receiptPhoto = null,
        )

        assertThat(response.requestPayload).contains("<base64")
        // 실제 Base64 문자열(binarydata 인코딩)이 그대로 노출되지 않아야 한다.
        assertThat(response.requestPayload).doesNotContain("YmluYXJ5ZGF0YQ")
    }

    @Test
    @DisplayName("검증 실패(개인카드인데 영수증 미첨부) — SF 호출 전 예외, 호출 안 함")
    fun test_validationFailsBeforeSfCall() {
        assertThatThrownBy {
            service.test(
                userId = 1L,
                request = newRequest(purchaseMethod = "B", amount = BigDecimal("10000")),
                claimPhoto = photo("claim"),
                partPhoto = photo("part"),
                receiptPhoto = null,
            )
        }.isInstanceOf(ReceiptRequiredException::class.java)

        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }

    @Test
    @DisplayName("SF 실패 응답 — success=false, resultCode/resultMsg 그대로 반환")
    fun test_sfFailureResponse() {
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "0", resultMsg = "DML 실패", rawBody = """{"RESULT_CODE":"0"}""")

        val response = service.test(
            userId = 1L,
            request = newRequest(),
            claimPhoto = photo("claim"),
            partPhoto = photo("part"),
            receiptPhoto = null,
        )

        assertThat(response.success).isFalse()
        assertThat(response.resultCode).isEqualTo("0")
        assertThat(response.resultMsg).isEqualTo("DML 실패")
    }
}
