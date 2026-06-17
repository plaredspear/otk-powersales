package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionDraftRequest
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionDraft
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionDraftRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@DisplayName("SuggestionDraftService 테스트 (제안 임시저장)")
class SuggestionDraftServiceTest {

    private val suggestionDraftRepository: SuggestionDraftRepository = mockk(relaxUnitFun = true)
    private val fileStorageService: FileStorageService = mockk()
    private val storageService: StorageService = mockk()
    private val service = SuggestionDraftService(suggestionDraftRepository, fileStorageService, storageService)

    private val userId = 100L

    private fun request() = SuggestionDraftRequest(
        category = "LOGISTICS_CLAIM",
        title = "포장 불량 제안",
        content = "박스가 찢어져 배송됨",
        productCode = "P0001",
        productName = "진라면",
        accountId = 5L,
        accountName = "테스트거래처",
        sapAccountCode = "0001234567",
        claimType = "파손",
        claimDate = LocalDate.of(2026, 6, 17),
        carNumber = "12가3456",
        logisticsResponsibility = "물류",
        duplicateProposalNum = null,
        actionStatus = null
    )

    @Test
    @DisplayName("임시저장이 없으면 getDraft 는 null 을 반환한다")
    fun getDraftReturnsNull() {
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns null

        assertThat(service.getDraft(userId)).isNull()
    }

    @Test
    @DisplayName("저장된 임시저장은 입력값과 사진 presigned URL 을 함께 반환한다")
    fun getDraftReturnsResponse() {
        val draft = SuggestionDraft(employeeId = userId).apply {
            category = "LOGISTICS_CLAIM"
            title = "포장 불량 제안"
            accountName = "테스트거래처"
            photoKey1 = "private/suggestion/a.jpg"
        }
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns draft
        every { storageService.getPresignedUrl("private/suggestion/a.jpg", any()) } returns "https://signed/a.jpg"

        val result = service.getDraft(userId)!!

        assertThat(result.title).isEqualTo("포장 불량 제안")
        assertThat(result.accountName).isEqualTo("테스트거래처")
        assertThat(result.photoUrls).containsExactly("https://signed/a.jpg")
    }

    @Test
    @DisplayName("임시저장이 없으면 새 row 를 insert(upsert)하고 검증 없이 저장한다")
    fun saveDraftInsertsWhenAbsent() {
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns null
        every { suggestionDraftRepository.save(any<SuggestionDraft>()) } answers { firstArg() }

        val result = service.saveDraft(userId, request(), null)

        assertThat(result.accountId).isEqualTo(5L)
        assertThat(result.category).isEqualTo("LOGISTICS_CLAIM")
        assertThat(result.claimDate).isEqualTo("2026-06-17")
        assertThat(result.photoUrls).isEmpty()
        verify { suggestionDraftRepository.save(any<SuggestionDraft>()) }
    }

    @Test
    @DisplayName("기존 임시저장이 있으면 같은 row 를 갱신한다")
    fun saveDraftUpdatesWhenPresent() {
        val existing = SuggestionDraft(employeeId = userId).apply { content = "이전내용" }
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns existing
        every { suggestionDraftRepository.save(any<SuggestionDraft>()) } answers { firstArg() }

        service.saveDraft(userId, request(), null)

        assertThat(existing.content).isEqualTo("박스가 찢어져 배송됨")
        verify { suggestionDraftRepository.save(existing) }
    }

    @Test
    @DisplayName("전달된 사진 2장은 업로드되어 photoKey1/2 로 저장된다")
    fun saveDraftUploadsProvidedPhotos() {
        val photo1: MultipartFile = mockk()
        val photo2: MultipartFile = mockk()
        every { photo1.isEmpty } returns false
        every { photo2.isEmpty } returns false
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns null
        every { suggestionDraftRepository.save(any<SuggestionDraft>()) } answers { firstArg() }
        every { fileStorageService.uploadSuggestionPhoto(photo1, 0L) } returns "private/suggestion/1.jpg"
        every { fileStorageService.uploadSuggestionPhoto(photo2, 0L) } returns "private/suggestion/2.jpg"
        every { storageService.getPresignedUrl("private/suggestion/1.jpg", any()) } returns "https://signed/1.jpg"
        every { storageService.getPresignedUrl("private/suggestion/2.jpg", any()) } returns "https://signed/2.jpg"

        val result = service.saveDraft(userId, request(), listOf(photo1, photo2))

        assertThat(result.photoUrls).containsExactly("https://signed/1.jpg", "https://signed/2.jpg")
        verify { fileStorageService.uploadSuggestionPhoto(photo1, 0L) }
        verify { fileStorageService.uploadSuggestionPhoto(photo2, 0L) }
    }

    @Test
    @DisplayName("사진 part 가 없으면 기존 사진 key 를 유지한다")
    fun saveDraftRetainsPhotosWhenAbsent() {
        val existing = SuggestionDraft(employeeId = userId).apply { photoKey1 = "private/suggestion/keep.jpg" }
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns existing
        every { suggestionDraftRepository.save(any<SuggestionDraft>()) } answers { firstArg() }
        every { storageService.getPresignedUrl("private/suggestion/keep.jpg", any()) } returns "https://signed/keep.jpg"

        val result = service.saveDraft(userId, request(), null)

        assertThat(result.photoUrls).containsExactly("https://signed/keep.jpg")
    }

    @Test
    @DisplayName("deleteDraft 는 존재하는 row 를 삭제한다")
    fun deleteDraftDeletes() {
        val draft = SuggestionDraft(employeeId = userId)
        every { suggestionDraftRepository.findByEmployeeId(userId) } returns draft

        service.deleteDraft(userId)

        verify { suggestionDraftRepository.delete(draft) }
    }
}
