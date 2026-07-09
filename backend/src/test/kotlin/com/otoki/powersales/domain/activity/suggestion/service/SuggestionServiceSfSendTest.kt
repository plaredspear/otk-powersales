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
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

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
    private val validator: SuggestionValidator = mockk()
    private val storageService: StorageService = mockk(relaxUnitFun = true)
    private val sfOutboundClient: SfOutboundClient = mockk()
    private val txTemplate: TransactionTemplate = mockk()

    private val service = SuggestionService(
        suggestionRepository, suggestionDraftRepository, uploadFileRepository, accountRepository,
        employeeRepository, productRepository, orgCostCenterMatchService, fileStorageService, validator, storageService,
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
