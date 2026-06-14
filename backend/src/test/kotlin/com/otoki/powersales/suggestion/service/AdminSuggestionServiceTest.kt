package com.otoki.powersales.suggestion.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionCreateRequest
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionUpdateRequest
import com.otoki.powersales.suggestion.entity.Suggestion
import com.otoki.powersales.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import com.otoki.powersales.suggestion.entity.SuggestionStatus
import com.otoki.powersales.suggestion.exception.InvalidSuggestionIdException
import com.otoki.powersales.suggestion.exception.InvalidSuggestionPhotoIdException
import com.otoki.powersales.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.suggestion.exception.SuggestionPhotoNotFoundException
import com.otoki.powersales.suggestion.repository.SuggestionRepository
import com.querydsl.core.types.dsl.Expressions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.Optional

@DisplayName("AdminSuggestionService 테스트 (Spec #830 P1-B)")
class AdminSuggestionServiceTest {

    private val suggestionRepository: SuggestionRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val orgCostCenterMatchService: OrgCostCenterMatchService = mockk()
    private val fileStorageService: FileStorageService = mockk(relaxUnitFun = true)
    private val validator: SuggestionValidator = mockk(relaxUnitFun = true)
    private val suggestionService: SuggestionService = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()

    private val service = AdminSuggestionService(
        suggestionRepository,
        uploadFileRepository,
        accountRepository,
        employeeRepository,
        productRepository,
        orgCostCenterMatchService,
        fileStorageService,
        validator,
        suggestionService,
        policyEvaluator
    )

    /** SF 가시 범위 평가 결과 — 테스트에서는 항상-true Predicate stub (필터/페이징 검증에 집중). */
    private val allowAllScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    private val adminId = 1L
    private val targetEmployeeId = 100L
    private val otherEmployeeId = 200L
    private val suggestionId = 999L
    private val photoId = 7L

    private lateinit var adminEmployee: Employee
    private lateinit var otherEmployee: Employee

    @BeforeEach
    fun setup() {
        adminEmployee = Employee(id = adminId, employeeCode = "ADM01", name = "운영자")
        otherEmployee = Employee(id = otherEmployeeId, employeeCode = "EMP002", name = "타인")
        // suggestionService helper 모킹 — 결과값만 확인하므로 단순 stub
        every { suggestionService.composeS3Url(any()) } answers { "https://test/${firstArg<String>()}" }
        every { suggestionService.formatFileSize(any()) } returns "1.0KB"
        every { suggestionService.computeWerkCenters(any(), any()) } returns (null to null)
        every { suggestionService.generateProposalNumber(any()) } returns "S-20260527-000001"
        // SF 가시 범위 Predicate stub — evaluator 자체 로직은 SharingRulePolicyEvaluator 테스트에서 검증.
        every { policyEvaluator.buildPredicate(any(), any(), any()) } returns Expressions.asBoolean(true).isTrue
        // 단건 가시성 — 기본 stub 은 가시(true). 가시 범위 밖 케이스에서 개별 override.
        every { suggestionRepository.existsVisibleById(any(), any()) } returns true
    }

    private fun suggestionOf(employee: Employee?, account: Account? = null): Suggestion = Suggestion(
        id = suggestionId,
        proposalNumber = "S-20260527-000001",
        title = "테스트 제안",
        content = "본문",
        category = SuggestionCategory.LOGISTICS_CLAIM,
        status = SuggestionStatus.SUBMITTED,
        isDeleted = false,
        employee = employee,
        account = account
    )

    private fun photoOf(deleted: Boolean = false, key: String = "uploads/suggestion/x.jpg"): UploadFile = UploadFile(
        id = photoId,
        uniqueKey = key,
        parentType = UploadFileParentTypes.SUGGESTION,
        parentId = suggestionId,
        isDeleted = deleted
    )

    @Nested
    @DisplayName("search - 목록 조회")
    inner class SearchTests {
        @Test
        @DisplayName("기본 조회 — 최근 30일 기간 default, 필터 없음")
        fun defaultSearch() {
            val page = PageImpl(listOf(suggestionOf(otherEmployee)), PageRequest.of(0, 20), 1L)
            every { suggestionRepository.searchForAdmin(any(), any(), any()) } returns page

            val result = service.search(allowAllScope, null, null, AdminSuggestionFilterParams(), 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].id).isEqualTo(suggestionId)
            assertThat(result.totalElements).isEqualTo(1L)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
        }

        @Test
        @DisplayName("종료일이 시작일보다 빠르면 IllegalArgumentException")
        fun invalidDateRange() {
            assertThatThrownBy {
                service.search(allowAllScope, LocalDate.of(2026, 5, 27), LocalDate.of(2026, 5, 26), AdminSuggestionFilterParams(), 0, 20)
            }.isInstanceOf(IllegalArgumentException::class.java)
            verify(exactly = 0) { suggestionRepository.searchForAdmin(any(), any(), any()) }
        }

        @Test
        @DisplayName("size 100 초과 시 100 으로 cap")
        fun sizeClampedTo100() {
            val captured = slot<org.springframework.data.domain.Pageable>()
            every { suggestionRepository.searchForAdmin(any(), any(), capture(captured)) } returns
                PageImpl(emptyList<Suggestion>(), PageRequest.of(0, 100), 0L)

            service.search(allowAllScope, null, null, AdminSuggestionFilterParams(), 0, 500)

            assertThat(captured.captured.pageSize).isEqualTo(100)
        }

        @Test
        @DisplayName("필터 — category + employeeName 전달")
        fun filterPropagation() {
            val captured = slot<AdminSuggestionFilter>()
            every { suggestionRepository.searchForAdmin(any(), capture(captured), any()) } returns
                PageImpl(emptyList<Suggestion>(), PageRequest.of(0, 20), 0L)

            service.search(
                allowAllScope, null, null,
                AdminSuggestionFilterParams(category = SuggestionCategory.LOGISTICS_CLAIM, employeeName = "홍"),
                0, 20
            )

            assertThat(captured.captured.category).isEqualTo(SuggestionCategory.LOGISTICS_CLAIM)
            assertThat(captured.captured.employeeName).isEqualTo("홍")
        }

        @Test
        @DisplayName("SF 가시 범위 — evaluator 에 scope + DKRetail__Proposal__c 전달, 산출 Predicate 를 repository 에 합성")
        fun appliesSharingPolicy() {
            val scope = DataScope(branchCodes = listOf("GHD03"), isAllBranches = false, userId = 42L)
            val policy = Expressions.asBoolean(true).isTrue
            val scopeSlot = slot<DataScope>()
            val sObjectSlot = slot<String>()
            every { policyEvaluator.buildPredicate(capture(scopeSlot), capture(sObjectSlot), any()) } returns policy
            val predicateSlot = slot<com.querydsl.core.types.Predicate>()
            every { suggestionRepository.searchForAdmin(capture(predicateSlot), any(), any()) } returns
                PageImpl(emptyList<Suggestion>(), PageRequest.of(0, 20), 0L)

            service.search(scope, null, null, AdminSuggestionFilterParams(), 0, 20)

            assertThat(sObjectSlot.captured).isEqualTo("DKRetail__Proposal__c")
            assertThat(scopeSlot.captured.userId).isEqualTo(42L)
            assertThat(scopeSlot.captured.branchCodes).containsExactly("GHD03")
            assertThat(predicateSlot.captured).isEqualTo(policy)
        }
    }

    @Nested
    @DisplayName("getDetail - 단건 상세")
    inner class GetDetailTests {
        @Test
        @DisplayName("정상 - SF 가시 범위 내 레코드 조회")
        fun returnsDetailWhenVisible() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns listOf(photoOf())

            val result = service.getDetail(allowAllScope, suggestionId)

            assertThat(result.id).isEqualTo(suggestionId)
            assertThat(result.attachments).hasSize(1)
            assertThat(result.attachments[0].s3Url).contains("uploads/suggestion/x.jpg")
        }

        @Test
        @DisplayName("SF 가시 범위 밖 — SuggestionNotFoundException (404, SF OWD=Private 동등)")
        fun throwsWhenNotVisible() {
            every { suggestionRepository.existsVisibleById(suggestionId, any()) } returns false

            assertThatThrownBy { service.getDetail(allowAllScope, suggestionId) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
            verify(exactly = 0) { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) }
        }

        @Test
        @DisplayName("id <= 0 → InvalidSuggestionIdException")
        fun rejectsInvalidId() {
            assertThatThrownBy { service.getDetail(allowAllScope, 0L) }
                .isInstanceOf(InvalidSuggestionIdException::class.java)
        }

        @Test
        @DisplayName("없는 id → SuggestionNotFoundException")
        fun throwsWhenMissing() {
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns null
            assertThatThrownBy { service.getDetail(allowAllScope, suggestionId) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("create - 등록")
    inner class CreateTests {
        @Test
        @DisplayName("정상 - employeeId 명시 시 그 사원 작성자로 저장 + photos 처리")
        fun createsOnBehalfOfEmployee() {
            every { employeeRepository.findById(targetEmployeeId) } returns Optional.of(
                Employee(id = targetEmployeeId, employeeCode = "EMP100", name = "대상사원")
            )
            every { productRepository.findByProductCode(any()) } returns null
            val savedSuggestion = suggestionOf(adminEmployee)
            every { suggestionRepository.save(any<Suggestion>()) } returns savedSuggestion
            every { fileStorageService.uploadSuggestionPhoto(any(), any()) } returns "uploads/suggestion/new.jpg"
            every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg<UploadFile>().also { } }

            val photo: MultipartFile = MockMultipartFile("photos", "p.jpg", "image/jpeg", byteArrayOf(1, 2))
            val request = AdminSuggestionCreateRequest(
                category = SuggestionCategory.NEW_PRODUCT,
                title = "제목", content = "본문",
                employeeId = targetEmployeeId
            )
            val result = service.create(adminId, request, listOf(photo))

            assertThat(result.proposalNumber).isEqualTo("S-20260527-000001")
            assertThat(result.attachments).hasSize(1)
            verify { employeeRepository.findById(targetEmployeeId) }
        }

        @Test
        @DisplayName("employeeId null 시 admin 본인이 작성자")
        fun defaultsToAdminAsAuthor() {
            every { employeeRepository.findById(adminId) } returns Optional.of(adminEmployee)
            every { productRepository.findByProductCode(any()) } returns null
            every { suggestionRepository.save(any<Suggestion>()) } returns suggestionOf(adminEmployee)

            val request = AdminSuggestionCreateRequest(
                category = SuggestionCategory.NEW_PRODUCT,
                title = "제목", content = "본문"
            )
            service.create(adminId, request, null)

            verify { employeeRepository.findById(adminId) }
        }

        @Test
        @DisplayName("photos > 10 → IllegalArgumentException")
        fun rejectsTooManyPhotos() {
            val photos = (1..11).map { MockMultipartFile("p", "$it.jpg", "image/jpeg", byteArrayOf(1)) }
            val request = AdminSuggestionCreateRequest(
                category = SuggestionCategory.NEW_PRODUCT, title = "x", content = "y"
            )
            assertThatThrownBy { service.create(adminId, request, photos) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최대 10")
        }
    }

    @Nested
    @DisplayName("update - 수정")
    inner class UpdateTests {
        @Test
        @DisplayName("정상 - SF 가시 범위 내 레코드 수정")
        fun updatesWhenVisible() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every { suggestionRepository.save(any<Suggestion>()) } returns suggestion
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns emptyList()

            val request = AdminSuggestionUpdateRequest(
                category = SuggestionCategory.LOGISTICS_CLAIM,
                title = "수정 제목", content = "수정 본문",
                claimType = "포장상태", claimDate = LocalDate.now(),
                actionStatus = SuggestionActionStatus.IN_PROGRESS
            )
            val result = service.update(allowAllScope, suggestionId, request)

            assertThat(suggestion.title).isEqualTo("수정 제목")
            assertThat(suggestion.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
            assertThat(result.id).isEqualTo(suggestionId)
        }

        @Test
        @DisplayName("SF 가시 범위 밖 — SuggestionNotFoundException (수정 불가)")
        fun throwsWhenNotVisible() {
            every { suggestionRepository.existsVisibleById(suggestionId, any()) } returns false

            val request = AdminSuggestionUpdateRequest(
                category = SuggestionCategory.LOGISTICS_CLAIM, title = "x", content = "y"
            )
            assertThatThrownBy { service.update(allowAllScope, suggestionId, request) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
            verify(exactly = 0) { suggestionRepository.save(any<Suggestion>()) }
        }

        @Test
        @DisplayName("id <= 0 → InvalidSuggestionIdException")
        fun rejectsInvalidId() {
            assertThatThrownBy { service.update(allowAllScope, 0L, AdminSuggestionUpdateRequest(category = SuggestionCategory.LOGISTICS_CLAIM, title = "x", content = "y")) }
                .isInstanceOf(InvalidSuggestionIdException::class.java)
        }
    }

    @Nested
    @DisplayName("softDelete - 삭제")
    inner class SoftDeleteTests {
        @Test
        @DisplayName("정상 - is_deleted=true 갱신")
        fun marksDeleted() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            val captured = slot<Suggestion>()
            every { suggestionRepository.save(capture(captured)) } answers { captured.captured }

            service.softDelete(allowAllScope, suggestionId)

            assertThat(captured.captured.isDeleted).isTrue()
        }

        @Test
        @DisplayName("SF 가시 범위 밖 — SuggestionNotFoundException (삭제 불가)")
        fun throwsWhenNotVisible() {
            every { suggestionRepository.existsVisibleById(suggestionId, any()) } returns false

            assertThatThrownBy { service.softDelete(allowAllScope, suggestionId) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
            verify(exactly = 0) { suggestionRepository.save(any<Suggestion>()) }
        }

        @Test
        @DisplayName("없는 id → SuggestionNotFoundException")
        fun throwsWhenMissing() {
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns null
            assertThatThrownBy { service.softDelete(allowAllScope, suggestionId) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("uploadPhotos - 사진 추가 업로드")
    inner class UploadPhotosTests {
        @Test
        @DisplayName("정상 - 기존 + 신규 합이 10 이내 → 모두 업로드")
        fun uploadsWhenWithinLimit() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns listOf(photoOf())  // existing = 1
            every { fileStorageService.uploadSuggestionPhoto(any(), any()) } returns "uploads/suggestion/new.jpg"
            every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg<UploadFile>() }

            val photos = (1..3).map { MockMultipartFile("photos", "$it.jpg", "image/jpeg", byteArrayOf(1)) }
            val result = service.uploadPhotos(allowAllScope, suggestionId, photos)

            assertThat(result).hasSize(3)
            assertThat(result[0].sortOrder).isEqualTo(1)  // baseIndex = existing.size = 1
            assertThat(result[2].sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("기존 + 신규 합이 10 초과 → IllegalArgumentException")
        fun rejectsWhenOverLimit() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns (1..8).map { photoOf(key = "uploads/suggestion/$it.jpg") }  // existing = 8

            val photos = (1..3).map { MockMultipartFile("photos", "$it.jpg", "image/jpeg", byteArrayOf(1)) }
            assertThatThrownBy { service.uploadPhotos(allowAllScope, suggestionId, photos) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최대 10")
            verify(exactly = 0) { fileStorageService.uploadSuggestionPhoto(any(), any()) }
        }

        @Test
        @DisplayName("빈 photos → IllegalArgumentException")
        fun rejectsEmptyPhotos() {
            assertThatThrownBy { service.uploadPhotos(allowAllScope, suggestionId, emptyList()) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("SF 가시 범위 밖 — SuggestionNotFoundException (사진 추가 불가)")
        fun throwsWhenNotVisible() {
            every { suggestionRepository.existsVisibleById(suggestionId, any()) } returns false

            val photos = listOf(MockMultipartFile("photos", "1.jpg", "image/jpeg", byteArrayOf(1)))
            assertThatThrownBy { service.uploadPhotos(allowAllScope, suggestionId, photos) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
            verify(exactly = 0) { fileStorageService.uploadSuggestionPhoto(any(), any()) }
        }
    }

    @Nested
    @DisplayName("deletePhoto - 단건 사진 삭제 (admin)")
    inner class DeletePhotoTests {
        @Test
        @DisplayName("정상 - S3 선행 삭제 + UploadFile.isDeleted=true")
        fun deletesPhoto() {
            val suggestion = suggestionOf(otherEmployee)
            val key = "uploads/suggestion/abc.jpg"
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns photoOf(key = key)
            val captured = slot<UploadFile>()
            every { uploadFileRepository.save(capture(captured)) } answers { captured.captured }

            service.deletePhoto(allowAllScope, suggestionId, photoId)

            verify(exactly = 1) { fileStorageService.deleteSuggestionPhoto(key) }
            assertThat(captured.captured.isDeleted).isTrue()
        }

        @Test
        @DisplayName("photo 미존재 → SuggestionPhotoNotFoundException")
        fun throwsWhenPhotoMissing() {
            val suggestion = suggestionOf(otherEmployee)
            every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
            } returns null

            assertThatThrownBy { service.deletePhoto(allowAllScope, suggestionId, photoId) }
                .isInstanceOf(SuggestionPhotoNotFoundException::class.java)
        }

        @Test
        @DisplayName("SF 가시 범위 밖 — SuggestionNotFoundException (사진 삭제 불가)")
        fun throwsWhenNotVisible() {
            every { suggestionRepository.existsVisibleById(suggestionId, any()) } returns false

            assertThatThrownBy { service.deletePhoto(allowAllScope, suggestionId, photoId) }
                .isInstanceOf(SuggestionNotFoundException::class.java)
            verify(exactly = 0) { fileStorageService.deleteSuggestionPhoto(any()) }
        }

        @Test
        @DisplayName("photoId <= 0 → InvalidSuggestionPhotoIdException")
        fun rejectsInvalidPhotoId() {
            assertThatThrownBy { service.deletePhoto(allowAllScope, suggestionId, 0L) }
                .isInstanceOf(InvalidSuggestionPhotoIdException::class.java)
        }
    }
}
