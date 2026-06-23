package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotResendableException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.product.entity.Product
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
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@DisplayName("AdminClaimResendService 테스트 — SF 재전송 (Spec #829)")
class AdminClaimResendServiceTest {

    private val claimRepository: ClaimRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val storageService: StorageService = mockk()
    private val sfOutboundClient: SfOutboundClient = mockk()
    private val txTemplate: TransactionTemplate = mockk()

    // SF 전송 로직은 실제 ClaimSfOutboundService — callApi/download mock 으로 동작 제어.
    private val sfOutboundService = ClaimSfOutboundService(storageService, sfOutboundClient)
    private val service = AdminClaimResendService(
        claimRepository, uploadFileRepository, sfOutboundService, txTemplate,
    )

    private val claimId = 42L

    @BeforeEach
    fun setup() {
        // txTemplate.execute { ... } 가 람다를 실제 실행하도록 stub.
        every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
        // pushToSfFromStored → S3 이미지 회수.
        every { storageService.downloadPrivate(any()) } returns byteArrayOf(1, 2, 3)
    }

    private fun sendFailedClaim(channel: ClaimChannel = ClaimChannel.WEB) = Claim(
        id = claimId,
        employee = Employee(id = 1L, employeeCode = "EMP001", name = "홍길동"),
        account = Account(id = 1, name = "거래처", branchCode = "B001", externalKey = "SAP001"),
        product = Product(id = 1L, name = "제품", productCode = "P0001"),
        dateType = ClaimDateType.EXPIRY_DATE,
        date = LocalDate.of(2026, 1, 1),
        claimType1 = ClaimType1.A,
        claimType2 = ClaimType2.AA,
        defectDescription = "이물질",
        defectQuantity = BigDecimal.valueOf(2L),
        status = ClaimStatus.SEND_FAILED,
        channel = channel,
    )

    private fun photo(kbn: String, key: String) = UploadFile(
        name = "$kbn.jpg",
        uniqueKey = key,
        parentType = UploadFileParentTypes.CLAIM,
        parentId = claimId,
        uploadKbn = kbn,
        isDeleted = false,
    )

    private fun stubPhotos() {
        every {
            uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimId)
        } returns listOf(
            photo(UploadFileKbnTypes.CLAIM_DEFECT, "uploads/claim/defect.jpg"),
            photo(UploadFileKbnTypes.CLAIM_PART, "uploads/claim/part.jpg"),
        )
    }

    @Test
    @DisplayName("SEND_FAILED 재전송 성공 - status=SENT + sfResultCode 응답")
    fun resendSucceeds() {
        val claim = sendFailedClaim()
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}")

        val result = service.resend(claimId)

        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
        assertThat(result.status).isEqualTo(ClaimStatus.SENT.name)
        assertThat(result.sfResultCode).isEqualTo("200")
    }

    @Test
    @DisplayName("SEND_FAILED 재전송 실패 - status=SEND_FAILED 유지 + 예외 미전파")
    fun resendKeepsFailedStatusOnSfError() {
        val claim = sendFailedClaim()
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } throws RuntimeException("timeout")

        val result = service.resend(claimId)

        assertThat(claim.status).isEqualTo(ClaimStatus.SEND_FAILED)
        assertThat(result.status).isEqualTo(ClaimStatus.SEND_FAILED.name)
    }

    @Test
    @DisplayName("등록 경로 channel 유지 - mobile(CAP) 재전송 시 payload Channel=CAP")
    fun resendPreservesOriginalChannel() {
        val claim = sendFailedClaim(channel = ClaimChannel.CAP)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}")

        service.resend(claimId)

        assertThat(apiMapSlot.captured["Channel"]).isEqualTo(ClaimChannel.CAP.name)
        assertThat(apiMapSlot.captured["EmployeeCode"]).isEqualTo("EMP001")
        assertThat(apiMapSlot.captured["SAPAccountCode"]).isEqualTo("SAP001")
    }

    @Test
    @DisplayName("SEND_FAILED 외 상태 - ClaimNotResendableException (SF 미호출)")
    fun rejectsNonSendFailedStatus() {
        val claim = sendFailedClaim().apply { status = ClaimStatus.SENT }
        every { claimRepository.findById(claimId) } returns Optional.of(claim)

        assertThatThrownBy { service.resend(claimId) }
            .isInstanceOf(ClaimNotResendableException::class.java)
        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }
}
