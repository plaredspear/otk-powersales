package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionCreateRequest
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionDraftRepository
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionSfRegistFailedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.Optional

@DisplayName("SuggestionService - SF ProposalRegist dual-write payload/상태 전이")
class SuggestionServiceSfSendTest {

    private val suggestionRepository: SuggestionRepository = mockk()
    private val suggestionDraftRepository: SuggestionDraftRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val orgCostCenterMatchService: OrgCostCenterMatchService = mockk()
    private val fileStorageService: FileStorageService = mockk(relaxUnitFun = true)
    private val photoUploader: SuggestionPhotoUploader = mockk()
    private val validator: SuggestionValidator = mockk()
    private val storageService: StorageService = mockk(relaxUnitFun = true)
    private val sfOutboundClient: SfOutboundClient = mockk()
    private val txTemplate: TransactionTemplate = mockk()

    private val service = SuggestionService(
        suggestionRepository, suggestionDraftRepository, uploadFileRepository, accountRepository,
        employeeRepository, productRepository, orgCostCenterMatchService, fileStorageService, photoUploader, validator, storageService,
        sfOutboundClient, txTemplate,
    )

    private fun request(
        sapAccountCode: String? = "SAP001",
        claimDate: LocalDate? = LocalDate.of(2026, 6, 23),
        carNumber: String? = "12가3456",
    ) = SuggestionCreateRequest(
        category = SuggestionCategory.LOGISTICS_CLAIM,
        title = "  제목  ",
        content = "  내용  ",
        productCode = "P001",
        accountId = 10L,
        sapAccountCode = sapAccountCode,
        claimType = "배송시간 지연",
        claimDate = claimDate,
        carNumber = carNumber,
    )

    private companion object {
        /** 파워세일즈 전용 버킷의 private key — SF 가 렌더하지 못하는 형식. */
        const val PRIVATE_KEY = "uploads/suggestion/2026/08/06/uuid.jpg"
    }

    @Nested
    @DisplayName("buildSfApiMap — 레거시 ProposalRegist Input key 정합 + pwrskey")
    inner class BuildApiMap {

        @Test
        fun `pwrskey 는 물류클레임 PK(suggestion_id) 를 문자열로 담는다`() {
            val map = service.buildSfApiMap(
                pwrskey = 12345L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(),
                employeeCode = "E777",
                photoMetas = emptyList(),
            )
            assertThat(map).containsEntry("pwrskey", "12345")
        }

        @Test
        fun `거래처는 소문자 accountCode 만 — SAPAccountCode·Type 미전송`() {
            val map = service.buildSfApiMap(
                pwrskey = 1L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(sapAccountCode = "SAP001"),
                employeeCode = "E1",
                photoMetas = emptyList(),
            )
            assertThat(map).containsEntry("accountCode", "SAP001")
            assertThat(map).doesNotContainKey("SAPAccountCode")
            assertThat(map).doesNotContainKey("Type")
        }

        @Test
        fun `기본 필드 trim·Category displayName·logclaimDate ISO 포맷`() {
            val map = service.buildSfApiMap(
                pwrskey = 1L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(),
                employeeCode = "E1",
                photoMetas = emptyList(),
            )
            assertThat(map).containsEntry("Category", "물류 클레임")
            assertThat(map).containsEntry("Title", "제목")
            assertThat(map).containsEntry("Description", "내용")
            assertThat(map).containsEntry("EmployeeCode", "E1")
            assertThat(map).containsEntry("claimList", "배송시간 지연")
            assertThat(map).containsEntry("logclaimDate", "2026-06-23")
        }

        @Test
        fun `미입력 값(sapAccountCode·claimDate)은 key 자체를 생략`() {
            val map = service.buildSfApiMap(
                pwrskey = 1L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(sapAccountCode = null, claimDate = null, carNumber = null),
                employeeCode = "E1",
                photoMetas = emptyList(),
            )
            assertThat(map).doesNotContainKey("accountCode")
            assertThat(map).doesNotContainKey("logclaimDate")
            assertThat(map).containsEntry("CarNumber", null)
        }

        @Test
        fun `이미지는 S3 key-only 1·2 슬롯에 채우고 없는 슬롯은 생략 — fileSize 는 레거시 포맷 문자열`() {
            val map = service.buildSfApiMap(
                pwrskey = 1L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(),
                employeeCode = "E1",
                photoMetas = listOf(
                    SuggestionService.SfPhotoMeta(uniqueKey = "uniq-1", fileSize = "200.0KB", fileName = "a.jpg"),
                ),
            )
            assertThat(map).containsEntry("S3ImageUniqueKey1", "uniq-1")
            assertThat(map).containsEntry("S3ImageFileName1", "a.jpg")
            // 레거시 ImageUtil.getFileSize() 포맷 문자열 그대로 (raw byte 정수 아님).
            assertThat(map).containsEntry("S3ImageFileSize1", "200.0KB")
            assertThat(map).doesNotContainKey("S3ImageUniqueKey2")
        }
    }

    @Nested
    @DisplayName("create — SF 로 보내는 이미지 key 는 SF 공유 버킷 사본")
    inner class CreatePhotoKey {

        /**
         * SF `IF_REST_MOBILE_ProposalRegist` 는 이미지 바이트를 받지 않고 UniqueKey 문자열만 `UploadFile__c` 에
         * 저장한 뒤, 렌더 시점에 레거시 공유 버킷 주소(`ottogi-nonsap-*-imagerepository-s3`)에 그 key 를 concat 한다.
         * 따라서 파워세일즈 전용 버킷의 private key 를 보내면 SF 화면의 이미지가 항상 깨진다.
         */
        @Test
        fun `S3ImageUniqueKey 는 공유 버킷 사본 key — private uploads 키를 보내지 않는다`() {
            val sent = stubCreateFlow(sfKey = "1750000000000E777_1")

            service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))

            assertThat(sent.captured["S3ImageUniqueKey1"]).isEqualTo("1750000000000E777_1")
            assertThat(sent.captured["S3ImageFileName1"]).isEqualTo("a_resize.jpg")
        }

        /** 공유 버킷 미설정 환경에서도 등록 자체는 진행 — 이미지 슬롯은 private key 로 fallback 한다. */
        @Test
        fun `공유 버킷 사본이 없으면 private key 로 fallback 하고 등록은 계속된다`() {
            val sent = stubCreateFlow(sfKey = null)

            service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))

            assertThat(sent.captured["S3ImageUniqueKey1"]).isEqualTo(PRIVATE_KEY)
        }

        /**
         * 레거시 `suggestProc` 은 `RESULT_CODE != 200` 이면 사용자에게 오류(E4/E5)를 반환하고 아무것도
         * 남기지 않았다. 신규도 동일하게 등록을 취소한다 — 목록에 "SF 에 없는 등록 건" 이 남으면 안 된다.
         */
        @Test
        fun `SF 전송 실패 시 등록도 실패한다 — SF RESULT_MSG 를 그대로 노출`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = false)
            stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }
                .isInstanceOf(SuggestionSfRegistFailedException::class.java)
                .hasMessage("잘못된 값입니다. (ProductCode)")
        }

        /** 등록 취소는 DB row 와 S3 객체(공유 사본 + private 사본)를 모두 되돌린다. */
        @Test
        fun `SF 전송 실패 시 suggestion·첨부·S3 객체를 되돌린다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = false)
            val files = stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }.isInstanceOf(SuggestionSfRegistFailedException::class.java)

            verify { fileStorageService.deleteSuggestionPhotoForSf("1750000000000E777") }
            verify { fileStorageService.deleteSuggestionPhoto(PRIVATE_KEY) }
            verify { uploadFileRepository.deleteAll(files) }
            verify { suggestionRepository.delete(any<Suggestion>()) }
        }

        /**
         * SF 는 DML 실패를 `RESULT_MSG='ERROR'` 로만 알린다 — 실제 사유(어느 필드가 왜 거부됐는지)는
         * SF 내부 `InternalExceptionLog__c` 에만 남는다. 그대로 노출하면 사용자가 할 수 있는 게 없으므로
         * 조치 가능한 안내로 바꾼다. 원문은 external_api_log.error_detail 에 그대로 보존된다.
         */
        @Test
        fun `SF 가 사유 없는 ERROR 만 주면 사용자에게는 관리자 문의 안내로 바꾼다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = true)
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("0", "ERROR", "{}")
            stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }
                .isInstanceOf(SuggestionSfRegistFailedException::class.java)
                .hasMessage("Salesforce 등록에 실패했습니다. 관리자에게 문의해주세요.")
        }

        /** Apex 예외 문자열도 사용자에겐 무의미하고 내부 구조가 노출되므로 감춘다. */
        @Test
        fun `Apex 예외 문자열도 일반 안내로 바꾼다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = true)
            every { sfOutboundClient.callApi(any(), any()) } returns
                SfApiResponse("0", "System.QueryException: List has no rows for assignment", "{}")
            stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }
                .isInstanceOf(SuggestionSfRegistFailedException::class.java)
                .hasMessage("Salesforce 등록에 실패했습니다. 관리자에게 문의해주세요.")
        }

        /** 반대로 사용자가 고칠 수 있는 메시지는 그대로 통과시킨다. */
        @Test
        fun `조치 가능한 SF 메시지는 그대로 노출한다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = false)
            stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }.hasMessage("잘못된 값입니다. (ProductCode)")
        }

        /** SF 응답 없이 호출 자체가 실패한 경우(타임아웃/OAuth)도 등록 실패 — 일반 안내 문구. */
        @Test
        fun `SF 호출 자체가 실패해도 등록은 취소된다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = true)
            every { sfOutboundClient.callApi(any(), any()) } throws RuntimeException("timeout")
            stubRollbackLookups()

            assertThatThrownBy {
                service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))
            }
                .isInstanceOf(SuggestionSfRegistFailedException::class.java)
                .hasMessageContaining("Salesforce 등록에 실패했습니다")
        }

        @Test
        fun `SF 전송 성공 시 등록이 유지된다`() {
            stubCreateFlow(sfKey = "1750000000000E777", sfSuccess = true)

            val result = service.create(1L, request(), listOf(MockMultipartFile("photos", "a.jpg", "image/jpeg", byteArrayOf(1))))

            assertThat(result.proposalNumber).isNotBlank()
            verify(exactly = 0) { suggestionRepository.delete(any<Suggestion>()) }
        }

        /** 보상 삭제 경로가 조회하는 첨부/제안 stub. */
        private fun stubRollbackLookups(): List<UploadFile> {
            val files = listOf(
                UploadFile(
                    name = "a_resize.jpg",
                    uniqueKey = PRIVATE_KEY,
                    sfUniqueKey = "1750000000000E777",
                    fileSize = "1.0KB",
                    parentType = UploadFileParentTypes.SUGGESTION,
                    parentId = 1L,
                )
            )
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, any())
            } returns files
            every { uploadFileRepository.deleteAll(any<List<UploadFile>>()) } returns Unit
            every { suggestionRepository.delete(any<Suggestion>()) } returns Unit
            return files
        }

        private fun stubCreateFlow(sfKey: String?, sfSuccess: Boolean = true): CapturingSlot<Map<String, Any?>> {
            every { validator.validate(any(), any(), any(), any(), any(), any()) } just Runs
            every { employeeRepository.findById(1L) } returns
                Optional.of(Employee(id = 1L, employeeCode = "E777", name = "사원"))
            every { productRepository.findByProductCode(any()) } returns null
            every { accountRepository.findById(any()) } returns Optional.empty()
            every { suggestionRepository.nextProposalNumberSeqValue() } returns 1L
            every { suggestionRepository.save(any<Suggestion>()) } answers { firstArg() }
            every { suggestionRepository.findByIdAndIsDeletedFalse(any()) } returns
                Suggestion(proposalNumber = "S-20260806-000001")
            every { suggestionDraftRepository.findByEmployeeId(1L) } returns null
            every { photoUploader.store(any(), any()) } returns SuggestionPhotoUploader.StoredPhoto(
                uniqueKey = PRIVATE_KEY,
                sfUniqueKey = sfKey,
                fileName = "a_resize.jpg",
                fileSize = "1.0KB",
            )
            every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg() }
            every { storageService.getPresignedUrl(any(), any()) } returns "https://presigned"
            every { txTemplate.execute<Any?>(any()) } answers {
                firstArg<TransactionCallback<Any?>>().doInTransaction(mockk(relaxed = true))
            }
            return slot<Map<String, Any?>>().also {
                every { sfOutboundClient.callApi(any(), capture(it)) } returns
                    if (sfSuccess) SfApiResponse("200", "OK", "{}") else SfApiResponse("0", "잘못된 값입니다. (ProductCode)", "{}")
            }
        }
    }

    @Nested
    @DisplayName("applySfResult — 전송상태 전이")
    inner class ApplyResult {

        @Test
        fun `성공 시 SENT + sf_sent_at 세팅 + 시도횟수 증가`() {
            val suggestion = Suggestion(proposalNumber = "S-20260623-000001")
            val result = SuggestionService.SfPushResult(
                success = true,
                apiResponse = SfApiResponse("200", "OK", "{}"),
                errorSummary = null,
            )
            service.applySfResult(suggestion, result)

            assertThat(suggestion.sfSendStatus).isEqualTo(SuggestionSfSendStatus.SENT)
            assertThat(suggestion.sfSentAt).isNotNull()
            assertThat(suggestion.sfSendFailMessage).isNull()
            assertThat(suggestion.sfSendAttemptCount).isEqualTo(1)
        }

        @Test
        fun `실패 시 SEND_FAILED + RESULT_MSG 박제 + 시도횟수 증가`() {
            val suggestion = Suggestion(proposalNumber = "S-20260623-000002")
            val result = SuggestionService.SfPushResult(
                success = false,
                apiResponse = SfApiResponse("500", "거래처를 찾을 수 없습니다", "{}"),
                errorSummary = null,
            )
            service.applySfResult(suggestion, result)

            assertThat(suggestion.sfSendStatus).isEqualTo(SuggestionSfSendStatus.SEND_FAILED)
            assertThat(suggestion.sfSendFailMessage).isEqualTo("거래처를 찾을 수 없습니다")
            assertThat(suggestion.sfSentAt).isNull()
            assertThat(suggestion.sfSendAttemptCount).isEqualTo(1)
        }

        @Test
        fun `실패 시 응답이 없으면 errorSummary 를 실패사유로 박제`() {
            val suggestion = Suggestion(proposalNumber = "S-20260623-000003")
            val result = SuggestionService.SfPushResult(
                success = false,
                apiResponse = null,
                errorSummary = "SF OAuth 토큰 발급에 실패했습니다",
            )
            service.applySfResult(suggestion, result)

            assertThat(suggestion.sfSendStatus).isEqualTo(SuggestionSfSendStatus.SEND_FAILED)
            assertThat(suggestion.sfSendFailMessage).isEqualTo("SF OAuth 토큰 발급에 실패했습니다")
        }
    }

    @Nested
    @DisplayName("invokeSf — SF 호출 실패는 예외를 던지지 않고 결과로 흡수")
    inner class InvokeSf {

        @Test
        fun `RESULT_CODE 200 이면 success`() {
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("200", "OK", "{}")
            val result = service.invokeSf(emptyMap())
            assertThat(result.success).isTrue()
        }

        @Test
        fun `RESULT_CODE 가 200 이 아니면 실패`() {
            every { sfOutboundClient.callApi(any(), any()) } returns SfApiResponse("500", "에러", "{}")
            val result = service.invokeSf(emptyMap())
            assertThat(result.success).isFalse()
        }

        @Test
        fun `SfOAuthFailedException 은 catch 되어 실패 결과로 흡수`() {
            every { sfOutboundClient.callApi(any(), any()) } throws SfOAuthFailedException("401")
            val result = service.invokeSf(emptyMap())
            assertThat(result.success).isFalse()
            assertThat(result.errorSummary).isNotNull()
        }

        @Test
        fun `일반 예외도 catch 되어 실패 결과로 흡수`() {
            every { sfOutboundClient.callApi(any(), any()) } throws RuntimeException("timeout")
            val result = service.invokeSf(emptyMap())
            assertThat(result.success).isFalse()
            assertThat(result.errorSummary).isEqualTo("timeout")
        }
    }

    @Nested
    @DisplayName("formatFileSize — 레거시 ImageUtil.getFileSize(long) 완전 재현")
    inner class FormatFileSize {

        @Test
        fun `KB 는 정수 절삭 — 1536B 는 1_5KB 가 아니라 1_0KB`() {
            // 레거시는 fileSize /= 1024 를 long 으로 수행해 소수부를 버린다(1536/1024=1).
            assertThat(service.formatFileSize(1536L)).isEqualTo("1.0KB")
        }

        @Test
        fun `정확히 나누어떨어지는 값은 그대로`() {
            assertThat(service.formatFileSize(204800L)).isEqualTo("200.0KB") // 200KB
            assertThat(service.formatFileSize(1024L)).isEqualTo("1.0KB")
        }

        @Test
        fun `1024 미만은 Byte 단위 (숫자는 _0 접미, 단위 앞 공백 없음)`() {
            assertThat(service.formatFileSize(512L)).isEqualTo("512.0Byte")
        }

        @Test
        fun `0 byte 는 루프 미진입 — 0_0Byte`() {
            assertThat(service.formatFileSize(0L)).isEqualTo("0.0Byte")
        }

        @Test
        fun `MB 단위 — 정수 절삭 유지`() {
            // 2 * 1024 * 1024 = 2097152 → 2회 나눗셈 후 2.0, 단위 MB.
            assertThat(service.formatFileSize(2L * 1024 * 1024)).isEqualTo("2.0MB")
            // 1.5MB 상당(1572864) → 1024 로 두 번 절삭: 1572864→1536→1 → "1.0MB"(레거시 정수절삭).
            assertThat(service.formatFileSize(1572864L)).isEqualTo("1.0MB")
        }

        @Test
        fun `GB 이상은 단위 배열 초과 예외 경로 — 0_0 Byte (공백 있음)`() {
            // 1GB = 1073741824 → Byte/KB/MB 를 넘어 index 3 접근 시 ArrayIndexOutOfBounds → catch.
            assertThat(service.formatFileSize(1024L * 1024 * 1024)).isEqualTo("0.0 Byte")
        }
    }
}
