package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@DisplayName("SuggestionSfResendService - SF ProposalRegist 재전송 payload/상태 전이")
class SuggestionSfResendServiceTest {

    private val suggestionRepository: SuggestionRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val photoUploader: SuggestionPhotoUploader = mockk()
    private val sfOutboundClient: SfOutboundClient = mockk()
    private val txTemplate: TransactionTemplate = mockk()

    private val service = SuggestionSfResendService(
        suggestionRepository, uploadFileRepository, photoUploader, sfOutboundClient, txTemplate,
    )

    private fun suggestion(
        id: Long = 12345L,
        sapAccountCode: String? = "SAP001",
        claimDate: LocalDate? = LocalDate.of(2026, 6, 23),
        carNumber: String? = "12가3456",
        employeeCode: String? = "E777",
        productCode: String? = "P001",
    ): Suggestion {
        val product = mockk<Product> { every { this@mockk.productCode } returns productCode }
        val employee = mockk<Employee> { every { this@mockk.employeeCode } returns employeeCode }
        return mockk {
            every { this@mockk.id } returns id
            every { category } returns SuggestionCategory.LOGISTICS_CLAIM
            every { title } returns "  제목  "
            every { content } returns "  내용  "
            every { this@mockk.product } returns product
            every { this@mockk.employee } returns employee
            every { this@mockk.sapAccountCode } returns sapAccountCode
            every { claimType } returns "배송시간 지연"
            every { this@mockk.claimDate } returns claimDate
            every { this@mockk.carNumber } returns carNumber
        }
    }

    /** Tx2(applySfResult) 가 건드리는 전송상태 필드 stub — payload 검증 테스트의 관심사가 아니다. */
    private fun stubStatusTransition(target: Suggestion) {
        every { target.sfSendAttemptCount } returns 0
        every { target.sfSendAttemptCount = any() } just Runs
        every { target.sfSendStatus = any() } just Runs
        every { target.sfSentAt = any() } just Runs
        every { target.sfSendFailMessage = any() } just Runs
    }

    @Nested
    @DisplayName("buildSfApiMap — 엔티티 복원 payload 정합")
    inner class BuildApiMap {

        @Test
        fun `pwrskey 와 기본 필드 trim·Category displayName·logclaimDate ISO`() {
            val map = service.buildSfApiMap(suggestion(id = 12345L), emptyList())
            assertThat(map).containsEntry("pwrskey", "12345")
            assertThat(map).containsEntry("Category", "물류 클레임")
            assertThat(map).containsEntry("Title", "제목")
            assertThat(map).containsEntry("Description", "내용")
            assertThat(map).containsEntry("EmployeeCode", "E777")
            assertThat(map).containsEntry("ProductCode", "P001")
            assertThat(map).containsEntry("claimList", "배송시간 지연")
            assertThat(map).containsEntry("logclaimDate", "2026-06-23")
            assertThat(map).containsEntry("accountCode", "SAP001")
            assertThat(map).doesNotContainKey("SAPAccountCode")
            assertThat(map).doesNotContainKey("Type")
        }

        @Test
        fun `미입력 값은 key 생략`() {
            val map = service.buildSfApiMap(
                suggestion(sapAccountCode = null, claimDate = null, carNumber = null),
                emptyList(),
            )
            assertThat(map).doesNotContainKey("accountCode")
            assertThat(map).doesNotContainKey("logclaimDate")
            assertThat(map).containsEntry("CarNumber", null)
        }

        @Test
        fun `이미지는 S3 key-only 슬롯 — fileSize 는 저장된 포맷 문자열 그대로`() {
            val map = service.buildSfApiMap(
                suggestion(),
                listOf(
                    SuggestionSfResendService.SfPhotoMeta(uniqueKey = "uniq-1", fileName = "a.jpg", fileSize = "200.0KB"),
                ),
            )
            assertThat(map).containsEntry("S3ImageUniqueKey1", "uniq-1")
            assertThat(map).containsEntry("S3ImageFileName1", "a.jpg")
            assertThat(map).containsEntry("S3ImageFileSize1", "200.0KB")
            assertThat(map).doesNotContainKey("S3ImageUniqueKey2")
        }
    }

    @Nested
    @DisplayName("applySfResult — 전송상태 전이")
    inner class ApplyResult {

        @Test
        fun `성공 시 SENT + sf_sent_at + 시도횟수 증가`() {
            val suggestion = Suggestion(proposalNumber = "S-20260623-000001")
            val result = SuggestionSfResendService.SfPushResult(true, SfApiResponse("200", "OK", "{}"), null)
            service.applySfResult(suggestion, result)

            assertThat(suggestion.sfSendStatus).isEqualTo(SuggestionSfSendStatus.SENT)
            assertThat(suggestion.sfSentAt).isNotNull()
            assertThat(suggestion.sfSendFailMessage).isNull()
            assertThat(suggestion.sfSendAttemptCount).isEqualTo(1)
        }

        @Test
        fun `실패 시 SEND_FAILED + RESULT_MSG 박제 + 시도횟수 증가`() {
            val suggestion = Suggestion(proposalNumber = "S-20260623-000002")
            val result = SuggestionSfResendService.SfPushResult(false, SfApiResponse("500", "거래처 없음", "{}"), null)
            service.applySfResult(suggestion, result)

            assertThat(suggestion.sfSendStatus).isEqualTo(SuggestionSfSendStatus.SEND_FAILED)
            assertThat(suggestion.sfSendFailMessage).isEqualTo("거래처 없음")
            assertThat(suggestion.sfSendAttemptCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("invokeSf — 실패는 예외 없이 결과 흡수")
    inner class InvokeSf {

        @Test
        fun `200 이면 success`() {
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("200", "OK", "{}")
            assertThat(service.invokeSf(emptyMap()).success).isTrue()
        }

        @Test
        fun `OAuth 실패 흡수`() {
            every { sfOutboundClient.callApi(any(), any()) } throws SfOAuthFailedException("401")
            val result = service.invokeSf(emptyMap())
            assertThat(result.success).isFalse()
            assertThat(result.errorSummary).isNotNull()
        }
    }

    @Nested
    @DisplayName("resend — 상태 가드")
    inner class Resend {

        private fun photo(sfKey: String?) = UploadFile(
            name = "a_resize.jpg",
            uniqueKey = "uploads/suggestion/2026/08/06/uuid.jpg",
            sfUniqueKey = sfKey,
            fileSize = "200.0KB",
            parentType = UploadFileParentTypes.SUGGESTION,
            parentId = 7L,
        )

        private fun stubTx() {
            every { txTemplate.execute<Any?>(any()) } answers {
                val cb = firstArg<org.springframework.transaction.support.TransactionCallback<Any?>>()
                cb.doInTransaction(mockk(relaxed = true))
            }
        }

        /**
         * SF 는 이미지 바이트를 받지 않고 UniqueKey 를 레거시 공유 버킷 주소에 concat 해 렌더한다.
         * 따라서 재전송 payload 도 파워세일즈 전용 버킷의 private key(unique_key) 가 아니라
         * 공유 버킷 사본 key 를 실어야 한다.
         */
        @Test
        fun `payload 이미지 key 는 공유 버킷 사본 key — private unique_key 를 보내지 않는다`() {
            val target = suggestion(id = 7L)
            every { target.sfSendStatus } returns SuggestionSfSendStatus.SEND_FAILED
            every { suggestionRepository.findByIdWithSfRefs(7L) } returns target
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, 7L)
            } returns listOf(photo(sfKey = "1750000000000E777"))
            every { photoUploader.ensureSfCopy(any(), "E777") } returns "1750000000000E777"
            stubStatusTransition(target)
            stubTx()
            val sent = slot<Map<String, Any?>>()
            every { sfOutboundClient.callApi(any(), capture(sent)) } returns SfApiResponse("200", "OK", "{}")

            service.resend(7L)

            assertThat(sent.captured["S3ImageUniqueKey1"]).isEqualTo("1750000000000E777")
            assertThat(sent.captured["S3ImageFileName1"]).isEqualTo("a_resize.jpg")
        }

        /**
         * 직전 실패에서 공유 사본을 회수했으므로(레거시 RESULT_CODE != 200 분기) 재전송은 private 사본으로
         * 사본을 다시 만든 뒤 그 key 를 보낸다 — 회수 때문에 이미지가 영구히 빠지면 안 된다.
         */
        @Test
        fun `회수된 사본은 재생성한 key 로 전송된다`() {
            val target = suggestion(id = 8L)
            every { target.sfSendStatus } returns SuggestionSfSendStatus.SEND_FAILED
            every { suggestionRepository.findByIdWithSfRefs(8L) } returns target
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, 8L)
            } returns listOf(photo(sfKey = null))
            every { photoUploader.ensureSfCopy(any(), "E777") } returns "1760000000000E777"
            stubStatusTransition(target)
            stubTx()
            val sent = slot<Map<String, Any?>>()
            every { sfOutboundClient.callApi(any(), capture(sent)) } returns SfApiResponse("200", "OK", "{}")

            service.resend(8L)

            assertThat(sent.captured["S3ImageUniqueKey1"]).isEqualTo("1760000000000E777")
        }

        /** 레거시 `suggestProc` 의 RESULT_CODE != 200 분기 — 공유 버킷에 고아 이미지를 남기지 않는다. */
        @Test
        fun `전송 실패 시 공유 버킷 사본을 회수한다`() {
            val target = suggestion(id = 9L)
            every { target.sfSendStatus } returns SuggestionSfSendStatus.SEND_FAILED
            every { suggestionRepository.findByIdWithSfRefs(9L) } returns target
            val files = listOf(photo(sfKey = "1750000000000E777"))
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, 9L)
            } returns files
            every { photoUploader.ensureSfCopy(any(), "E777") } returns "1750000000000E777"
            every { photoUploader.discardSfCopies(any()) } just Runs
            stubStatusTransition(target)
            stubTx()
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("500", "거래처 없음", "{}")

            service.resend(9L)

            verify { photoUploader.discardSfCopies(files) }
        }

        /** 성공 시에는 회수하지 않는다 — SF 가 그 key 로 이미지를 렌더한다. */
        @Test
        fun `전송 성공 시 공유 버킷 사본을 유지한다`() {
            val target = suggestion(id = 10L)
            every { target.sfSendStatus } returns SuggestionSfSendStatus.SEND_FAILED
            every { suggestionRepository.findByIdWithSfRefs(10L) } returns target
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, 10L)
            } returns listOf(photo(sfKey = "1750000000000E777"))
            every { photoUploader.ensureSfCopy(any(), "E777") } returns "1750000000000E777"
            stubStatusTransition(target)
            stubTx()
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("200", "OK", "{}")

            service.resend(10L)

            verify(exactly = 0) { photoUploader.discardSfCopies(any()) }
        }

        @Test
        fun `SEND_FAILED 가 아니면 SF 호출 없이 skip`() {
            val sent = mockk<Suggestion> { every { sfSendStatus } returns SuggestionSfSendStatus.SENT }
            every { suggestionRepository.findByIdWithSfRefs(1L) } returns sent
            every { txTemplate.execute<Any?>(any()) } answers {
                val cb = firstArg<org.springframework.transaction.support.TransactionCallback<Any?>>()
                cb.doInTransaction(mockk(relaxed = true))
            }

            val result = service.resend(1L)

            assertThat(result).isNull()
        }
    }
}
