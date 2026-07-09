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
        fun `이미지는 S3 key-only 1·2 슬롯에 채우고 없는 슬롯은 생략`() {
            val map = service.buildSfApiMap(
                pwrskey = 1L,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                request = request(),
                employeeCode = "E1",
                photoMetas = listOf(
                    SuggestionService.SfPhotoMeta(uniqueKey = "uniq-1", fileSize = 1024L, fileName = "a.jpg"),
                ),
            )
            assertThat(map).containsEntry("S3ImageUniqueKey1", "uniq-1")
            assertThat(map).containsEntry("S3ImageFileName1", "a.jpg")
            assertThat(map).containsEntry("S3ImageFileSize1", "1024")
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
}
