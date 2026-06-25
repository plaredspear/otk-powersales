package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

/**
 * ClaimSfDispatchService 테스트 — claimId 기준 SF `/ClaimRegist` push + status 전이.
 *
 * 신규 등록 직후 비동기 송신([ClaimSfPushDispatcher])과 수동 재전송([AdminClaimResendService])이
 * 공유하는 전송 골격 (snapshot 복원 → S3 이미지 회수 → SF 호출 → status update) 을 검증한다.
 */
@DisplayName("ClaimSfDispatchService 테스트")
class ClaimSfDispatchServiceTest {

    private val claimRepository: ClaimRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val storageService: StorageService = mockk()
    private val sfOutboundClient: SfOutboundClient = mockk()
    private val txTemplate: TransactionTemplate = mockk()

    private val sfOutboundService = ClaimSfOutboundService(storageService, sfOutboundClient)
    private val service = ClaimSfDispatchService(
        claimRepository, uploadFileRepository, sfOutboundService, txTemplate,
    )

    private val claimId = 42L

    // 등록 직후 비동기 전송: SF_PENDING 허용. 재전송: SEND_FAILED 허용.
    private val pendingAllowed = setOf(ClaimStatus.SF_PENDING)
    private val resendAllowed = setOf(ClaimStatus.SEND_FAILED)

    @BeforeEach
    fun setup() {
        every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
        every { storageService.downloadPrivate(any()) } returns byteArrayOf(1, 2, 3)
    }

    private fun claimWith(
        status: ClaimStatus,
        channel: ClaimChannel = ClaimChannel.WEB,
    ) = Claim(
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
        status = status,
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
    @DisplayName("SF_PENDING dispatch 성공 → status=SENT + sendAttemptCount 1")
    fun dispatchPendingSucceeds() {
        val claim = claimWith(ClaimStatus.SF_PENDING)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}")

        val result = service.dispatch(claimId, pendingAllowed, onStatusMismatch = { error("호출되면 안 됨") })

        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
        assertThat(claim.sendAttemptCount).isEqualTo(1)
        assertThat(claim.sentAt).isNotNull()
        assertThat(result?.status).isEqualTo(ClaimStatus.SENT)
        assertThat(result?.sfResult?.success).isTrue()
    }

    @Test
    @DisplayName("SF 호출 실패 → status=SEND_FAILED + 예외 미전파")
    fun dispatchKeepsFailedStatusOnSfError() {
        val claim = claimWith(ClaimStatus.SF_PENDING)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        every { sfOutboundClient.callApi("/ClaimRegist", any()) } throws RuntimeException("timeout")

        val result = service.dispatch(claimId, pendingAllowed, onStatusMismatch = { error("호출되면 안 됨") })

        assertThat(claim.status).isEqualTo(ClaimStatus.SEND_FAILED)
        assertThat(result?.status).isEqualTo(ClaimStatus.SEND_FAILED)
        assertThat(result?.sfResult?.success).isFalse()
    }

    @Test
    @DisplayName("apiMap 정합 — Channel/pwrskey/EmployeeCode/SAPAccountCode + ExpirationDate, ManufacturingDate=\"\"")
    fun dispatchBuildsApiMap() {
        val claim = claimWith(ClaimStatus.SF_PENDING, channel = ClaimChannel.WEB)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}")

        service.dispatch(claimId, pendingAllowed, onStatusMismatch = { error("호출되면 안 됨") })

        assertThat(apiMapSlot.captured["Channel"]).isEqualTo("WEB")
        assertThat(apiMapSlot.captured["EmployeeCode"]).isEqualTo("EMP001")
        assertThat(apiMapSlot.captured["SAPAccountCode"]).isEqualTo("SAP001")
        // pwrskey — 대상 claim PK 를 문자열로 전송 (SF 역연결용)
        assertThat(apiMapSlot.captured["pwrskey"]).isEqualTo(claimId.toString())
        assertThat(apiMapSlot.captured["ExpirationDate"]).isEqualTo("2026-01-01")
        assertThat(apiMapSlot.captured["ManufacturingDate"]).isEqualTo("")
    }

    @Test
    @DisplayName("재전송 경로 channel 유지 - mobile(CAP) 재전송 시 payload Channel=CAP")
    fun dispatchPreservesOriginalChannel() {
        val claim = claimWith(ClaimStatus.SEND_FAILED, channel = ClaimChannel.CAP)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        stubPhotos()
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/ClaimRegist", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}")

        service.dispatch(claimId, resendAllowed, onStatusMismatch = { error("호출되면 안 됨") })

        assertThat(apiMapSlot.captured["Channel"]).isEqualTo(ClaimChannel.CAP.name)
    }

    @Test
    @DisplayName("허용 상태 외 - onStatusMismatch 위임 + SF 미호출 + null 반환")
    fun dispatchSkipsWhenStatusMismatch() {
        // SENT 상태인 claim 에 재전송(SEND_FAILED 만 허용) 시도 → skip.
        val claim = claimWith(ClaimStatus.SENT)
        every { claimRepository.findById(claimId) } returns Optional.of(claim)
        var mismatchStatus: ClaimStatus? = null

        val result = service.dispatch(claimId, resendAllowed, onStatusMismatch = { mismatchStatus = it })

        assertThat(result).isNull()
        assertThat(mismatchStatus).isEqualTo(ClaimStatus.SENT)
        verify(exactly = 0) { sfOutboundClient.callApi(any(), any()) }
    }
}
