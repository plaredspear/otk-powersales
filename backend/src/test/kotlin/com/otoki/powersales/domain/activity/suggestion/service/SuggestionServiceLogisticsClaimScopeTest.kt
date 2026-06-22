package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionAccessDeniedException
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionDraftRepository
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.util.Optional

@DisplayName("SuggestionService - 물류클레임 조장 권한분기 (레거시 LogisticsClaimSearch 동등)")
class SuggestionServiceLogisticsClaimScopeTest {

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

    private val service = SuggestionService(
        suggestionRepository, suggestionDraftRepository, uploadFileRepository, accountRepository,
        employeeRepository, productRepository, orgCostCenterMatchService, fileStorageService, validator, storageService
    )

    private val leaderId = 10L
    private val womanId = 20L
    private val orgCc = "CC-100"

    private fun leader() =
        Employee(id = leaderId, employeeCode = "L001", name = "조장", role = AppAuthority.LEADER, costCenterCode = "RAW-100")

    private fun woman() =
        Employee(id = womanId, employeeCode = "W001", name = "여사원", role = AppAuthority.WOMAN, costCenterCode = "RAW-100")

    private fun logisticsClaim(id: Long, owner: Employee?, org: String? = orgCc) = Suggestion(
        id = id, proposalNumber = "S-$id", title = "t", content = "c",
        category = SuggestionCategory.LOGISTICS_CLAIM, status = SuggestionStatus.SUBMITTED,
        isDeleted = false, employee = owner, orgCostCenterCode = org,
    )

    @Test
    @DisplayName("목록 - 조장은 같은 원가센터 물류클레임 전체를 조회한다(scope=원가센터)")
    fun leaderListsCostCenterWide() {
        every { employeeRepository.findById(leaderId) } returns Optional.of(leader())
        every { orgCostCenterMatchService.findMatchingCostCenterCode("RAW-100") } returns Optional.of(orgCc)
        every {
            suggestionRepository.searchMine(
                employeeId = leaderId,
                scopeOrgCostCenterCode = orgCc,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = any(),
                createdFrom = any(),
                createdToExclusive = any(),
                pageable = any()
            )
        } returns PageImpl(listOf(logisticsClaim(1L, woman())))

        val result = service.listMine(leaderId, 0, 20, SuggestionCategory.LOGISTICS_CLAIM)

        assertThat(result.content).hasSize(1)
        verify(exactly = 1) {
            suggestionRepository.searchMine(
                employeeId = leaderId,
                scopeOrgCostCenterCode = orgCc,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = any(),
                createdFrom = any(),
                createdToExclusive = any(),
                pageable = any()
            )
        }
    }

    @Test
    @DisplayName("목록 - 여사원은 본인 물류클레임만 조회한다(scope=본인)")
    fun womanListsOwnOnly() {
        every { employeeRepository.findById(womanId) } returns Optional.of(woman())
        every { orgCostCenterMatchService.findMatchingCostCenterCode(any()) } returns Optional.of(orgCc)
        every {
            suggestionRepository.searchMine(
                employeeId = womanId,
                scopeOrgCostCenterCode = null,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = any(),
                createdFrom = any(),
                createdToExclusive = any(),
                pageable = any()
            )
        } returns PageImpl(emptyList())

        service.listMine(womanId, 0, 20, SuggestionCategory.LOGISTICS_CLAIM)

        verify(exactly = 1) {
            suggestionRepository.searchMine(
                employeeId = womanId,
                scopeOrgCostCenterCode = null,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = any(),
                createdFrom = any(),
                createdToExclusive = any(),
                pageable = any()
            )
        }
    }

    @Test
    @DisplayName("목록 - 거래처·기간 필터가 searchMine 으로 전달된다(기간=등록일, 종료일 익일 미만)")
    fun listPassesAccountAndDateFilters() {
        every { employeeRepository.findById(leaderId) } returns Optional.of(leader())
        every { orgCostCenterMatchService.findMatchingCostCenterCode("RAW-100") } returns Optional.of(orgCc)
        every {
            suggestionRepository.searchMine(
                employeeId = leaderId,
                scopeOrgCostCenterCode = orgCc,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = 77L,
                createdFrom = java.time.LocalDate.of(2026, 6, 1).atStartOfDay(),
                createdToExclusive = java.time.LocalDate.of(2026, 6, 30).plusDays(1).atStartOfDay(),
                pageable = any()
            )
        } returns PageImpl(emptyList())

        service.listMine(
            employeeId = leaderId,
            page = 0,
            size = 20,
            category = SuggestionCategory.LOGISTICS_CLAIM,
            accountId = 77L,
            startDate = java.time.LocalDate.of(2026, 6, 1),
            endDate = java.time.LocalDate.of(2026, 6, 30)
        )

        verify(exactly = 1) {
            suggestionRepository.searchMine(
                employeeId = leaderId,
                scopeOrgCostCenterCode = orgCc,
                category = SuggestionCategory.LOGISTICS_CLAIM,
                accountId = 77L,
                createdFrom = java.time.LocalDate.of(2026, 6, 1).atStartOfDay(),
                createdToExclusive = java.time.LocalDate.of(2026, 6, 30).plusDays(1).atStartOfDay(),
                pageable = any()
            )
        }
    }

    @Test
    @DisplayName("상세 - 조장은 같은 원가센터 타인 물류클레임을 조회하며 '오뚜기 접수사원'이 노출된다")
    fun leaderSeesReceptionEmployee() {
        val claim = logisticsClaim(5L, woman())
        every { suggestionRepository.findByIdAndIsDeletedFalse(5L) } returns claim
        every { employeeRepository.findById(leaderId) } returns Optional.of(leader())
        every { orgCostCenterMatchService.findMatchingCostCenterCode("RAW-100") } returns Optional.of(orgCc)
        every {
            uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, 5L)
        } returns emptyList()

        val response = service.getDetail(5L, leaderId)

        assertThat(response.receptionEmployeeName).isEqualTo("여사원")
        assertThat(response.receptionEmployeeCode).isEqualTo("W001")
    }

    @Test
    @DisplayName("상세 - 여사원은 타인 물류클레임 조회가 거부된다(본인분만)")
    fun womanDeniedOthersClaim() {
        val claim = logisticsClaim(6L, leader()) // 타인(조장) 소유
        every { suggestionRepository.findByIdAndIsDeletedFalse(6L) } returns claim
        every { employeeRepository.findById(womanId) } returns Optional.of(woman())
        every { orgCostCenterMatchService.findMatchingCostCenterCode(any()) } returns Optional.of(orgCc)

        assertThatThrownBy { service.getDetail(6L, womanId) }
            .isInstanceOf(SuggestionAccessDeniedException::class.java)
    }
}
