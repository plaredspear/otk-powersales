package com.otoki.powersales.suggestion.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.suggestion.entity.Suggestion
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import com.otoki.powersales.suggestion.entity.SuggestionStatus
import com.otoki.powersales.suggestion.exception.InvalidSuggestionIdException
import com.otoki.powersales.suggestion.exception.InvalidSuggestionPhotoIdException
import com.otoki.powersales.suggestion.exception.SuggestionAccessDeniedException
import com.otoki.powersales.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.suggestion.exception.SuggestionPhotoNotFoundException
import com.otoki.powersales.suggestion.repository.SuggestionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SuggestionService.deletePhoto - 첨부 사진 단건 삭제 (UC-06 / Spec #828)")
class SuggestionServiceDeletePhotoTest {

    private val suggestionRepository: SuggestionRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val orgCostCenterMatchService: OrgCostCenterMatchService = mockk()
    private val fileStorageService: FileStorageService = mockk(relaxUnitFun = true)
    private val validator: SuggestionValidator = mockk()
    private val storageService: StorageService = mockk(relaxUnitFun = true)

    private val suggestionService = SuggestionService(
        suggestionRepository,
        uploadFileRepository,
        accountRepository,
        employeeRepository,
        productRepository,
        orgCostCenterMatchService,
        fileStorageService,
        validator,
        storageService
    )

    private val employeeId = 100L
    private val otherEmployeeId = 200L
    private val suggestionId = 999L
    private val photoId = 7L

    private lateinit var owner: Employee
    private lateinit var otherOwner: Employee

    @BeforeEach
    fun setup() {
        owner = Employee(id = employeeId, employeeCode = "EMP001", name = "홍길동")
        otherOwner = Employee(id = otherEmployeeId, employeeCode = "EMP002", name = "김철수")
    }

    private fun suggestionOf(employee: Employee?): Suggestion = Suggestion(
        id = suggestionId,
        proposalNumber = "S-20260527-000001",
        title = "테스트 제안",
        content = "본문",
        category = SuggestionCategory.LOGISTICS_CLAIM,
        status = SuggestionStatus.SUBMITTED,
        isDeleted = false,
        employee = employee
    )

    private fun photoOf(key: String?, deleted: Boolean = false): UploadFile = UploadFile(
        id = photoId,
        uniqueKey = key,
        parentType = UploadFileParentTypes.SUGGESTION,
        parentId = suggestionId,
        isDeleted = deleted
    )

    @Test
    @DisplayName("정상 - 본인 row + 신규 S3 키 → S3 선행 삭제 + UploadFile.isDeleted=true")
    fun deletesPhotoOnHappyPath() {
        val key = "uploads/suggestion/2026/05/27/abc.jpg"
        val suggestion = suggestionOf(owner)
        val photo = photoOf(key)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
        every {
            uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
        } returns photo
        val saved = slot<UploadFile>()
        every { uploadFileRepository.save(capture(saved)) } answers { saved.captured }

        suggestionService.deletePhoto(employeeId, suggestionId, photoId)

        verify(exactly = 1) { fileStorageService.deleteSuggestionPhoto(key) }
        verify(exactly = 1) { uploadFileRepository.save(any<UploadFile>()) }
        assertThat(saved.captured.isDeleted).isTrue()
    }

    @Test
    @DisplayName("정상 - 레거시 uniqueKey 없는 row → S3 호출 생략 + isDeleted=true")
    fun skipsS3WhenKeyBlank() {
        val suggestion = suggestionOf(owner)
        val photo = photoOf(key = null)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
        every {
            uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
        } returns photo
        every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg() }

        suggestionService.deletePhoto(employeeId, suggestionId, photoId)

        verify(exactly = 0) { fileStorageService.deleteSuggestionPhoto(any()) }
        verify(exactly = 1) { uploadFileRepository.save(any<UploadFile>()) }
    }

    @Test
    @DisplayName("정상 - 레거시 마이그레이션 키(segment 없음) → FileStorageService 호출 (private/ 합성 삭제)")
    fun callsServiceForLegacyUuidKey() {
        val key = "178003103039220240015"
        val suggestion = suggestionOf(owner)
        val photo = photoOf(key)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
        every {
            uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
        } returns photo
        every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg() }

        suggestionService.deletePhoto(employeeId, suggestionId, photoId)

        verify(exactly = 1) { fileStorageService.deleteSuggestionPhoto(key) }
    }

    @Test
    @DisplayName("suggestionId <= 0 → InvalidSuggestionIdException")
    fun rejectsInvalidSuggestionId() {
        assertThatThrownBy { suggestionService.deletePhoto(employeeId, 0L, photoId) }
            .isInstanceOf(InvalidSuggestionIdException::class.java)
        verify(exactly = 0) { suggestionRepository.findByIdAndIsDeletedFalse(any()) }
    }

    @Test
    @DisplayName("photoId <= 0 → InvalidSuggestionPhotoIdException")
    fun rejectsInvalidPhotoId() {
        assertThatThrownBy { suggestionService.deletePhoto(employeeId, suggestionId, 0L) }
            .isInstanceOf(InvalidSuggestionPhotoIdException::class.java)
        verify(exactly = 0) { suggestionRepository.findByIdAndIsDeletedFalse(any()) }
    }

    @Test
    @DisplayName("suggestion 미존재 / soft-deleted → SuggestionNotFoundException")
    fun throwsWhenSuggestionMissing() {
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns null

        assertThatThrownBy { suggestionService.deletePhoto(employeeId, suggestionId, photoId) }
            .isInstanceOf(SuggestionNotFoundException::class.java)
        verify(exactly = 0) { fileStorageService.deleteSuggestionPhoto(any()) }
        verify(exactly = 0) { uploadFileRepository.save(any<UploadFile>()) }
    }

    @Test
    @DisplayName("다른 사원의 제안 → SuggestionAccessDeniedException")
    fun throwsWhenNotOwner() {
        val suggestion = suggestionOf(otherOwner)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion

        assertThatThrownBy { suggestionService.deletePhoto(employeeId, suggestionId, photoId) }
            .isInstanceOf(SuggestionAccessDeniedException::class.java)
        verify(exactly = 0) { uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(any(), any(), any()) }
        verify(exactly = 0) { fileStorageService.deleteSuggestionPhoto(any()) }
    }

    @Test
    @DisplayName("photo 미존재 / 이미 삭제 / 다른 suggestion 의 photo → SuggestionPhotoNotFoundException")
    fun throwsWhenPhotoMissing() {
        val suggestion = suggestionOf(owner)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
        every {
            uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
        } returns null

        assertThatThrownBy { suggestionService.deletePhoto(employeeId, suggestionId, photoId) }
            .isInstanceOf(SuggestionPhotoNotFoundException::class.java)
        verify(exactly = 0) { fileStorageService.deleteSuggestionPhoto(any()) }
        verify(exactly = 0) { uploadFileRepository.save(any<UploadFile>()) }
    }

    @Test
    @DisplayName("S3 장애 → 예외 전파 + DB save 미발화 (트랜잭션 롤백 보장)")
    fun propagatesS3Failure() {
        val key = "uploads/suggestion/2026/05/27/abc.jpg"
        val suggestion = suggestionOf(owner)
        val photo = photoOf(key)
        every { suggestionRepository.findByIdAndIsDeletedFalse(suggestionId) } returns suggestion
        every {
            uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestionId)
        } returns photo
        every { fileStorageService.deleteSuggestionPhoto(key) } throws RuntimeException("S3 down")

        assertThatThrownBy { suggestionService.deletePhoto(employeeId, suggestionId, photoId) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("S3 down")
        verify(exactly = 0) { uploadFileRepository.save(any<UploadFile>()) }
    }
}
