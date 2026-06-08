package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.dto.request.ClaimDraftRequest
import com.otoki.powersales.claim.entity.ClaimDraft
import com.otoki.powersales.claim.repository.ClaimDraftRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@DisplayName("ClaimDraftService 테스트 (클레임 임시저장)")
class ClaimDraftServiceTest {

    private val claimDraftRepository: ClaimDraftRepository = mockk(relaxUnitFun = true)
    private val fileStorageService: FileStorageService = mockk()
    private val storageService: StorageService = mockk()
    private val service = ClaimDraftService(claimDraftRepository, fileStorageService, storageService)

    private val userId = 100L

    private fun request() = ClaimDraftRequest(
        accountId = 5L,
        accountName = "테스트거래처",
        productCode = "P0001",
        productName = "진라면",
        dateType = "EXPIRY_DATE",
        date = "2026-06-08",
        claimType1 = "A",
        claimType2 = "AA",
        defectDescription = "이물질",
        defectQuantity = BigDecimal("3"),
        purchaseAmount = BigDecimal("1000"),
        purchaseMethodCode = "A",
        requestTypeCode = "교환"
    )

    @Test
    @DisplayName("임시저장이 없으면 getDraft 는 null 을 반환한다")
    fun getDraftReturnsNull() {
        every { claimDraftRepository.findByEmployeeId(userId) } returns null

        assertThat(service.getDraft(userId)).isNull()
    }

    @Test
    @DisplayName("저장된 임시저장은 코드/이름과 사진 presigned URL 을 함께 반환한다")
    fun getDraftReturnsResponse() {
        val draft = ClaimDraft(employeeId = userId).apply {
            accountId = 5L
            accountName = "테스트거래처"
            claimType1 = "A"
            defectPhotoKey = "uploads/claim/2026/06/08/a.jpg"
        }
        every { claimDraftRepository.findByEmployeeId(userId) } returns draft
        every { storageService.getPresignedUrl("uploads/claim/2026/06/08/a.jpg", any()) } returns "https://signed/a.jpg"

        val result = service.getDraft(userId)!!

        assertThat(result.accountName).isEqualTo("테스트거래처")
        assertThat(result.claimType1).isEqualTo("A")
        assertThat(result.defectPhotoUrl).isEqualTo("https://signed/a.jpg")
        assertThat(result.labelPhotoUrl).isNull()
    }

    @Test
    @DisplayName("임시저장이 없으면 새 row 를 insert(upsert)하고 검증 없이 저장한다")
    fun saveDraftInsertsWhenAbsent() {
        every { claimDraftRepository.findByEmployeeId(userId) } returns null
        every { claimDraftRepository.save(any<ClaimDraft>()) } answers { firstArg() }

        val result = service.saveDraft(userId, request(), null, null, null)

        assertThat(result.accountId).isEqualTo(5L)
        assertThat(result.claimType2).isEqualTo("AA")
        assertThat(result.date).isEqualTo("2026-06-08")
        verify { claimDraftRepository.save(any<ClaimDraft>()) }
    }

    @Test
    @DisplayName("기존 임시저장이 있으면 같은 row 를 갱신한다")
    fun saveDraftUpdatesWhenPresent() {
        val existing = ClaimDraft(employeeId = userId).apply { defectDescription = "이전내용" }
        every { claimDraftRepository.findByEmployeeId(userId) } returns existing
        every { claimDraftRepository.save(any<ClaimDraft>()) } answers { firstArg() }

        service.saveDraft(userId, request(), null, null, null)

        assertThat(existing.defectDescription).isEqualTo("이물질")
        verify { claimDraftRepository.save(existing) }
    }

    @Test
    @DisplayName("전달된 사진은 업로드되어 key 로 저장된다")
    fun saveDraftUploadsProvidedPhoto() {
        val photo: MultipartFile = mockk()
        every { photo.isEmpty } returns false
        every { claimDraftRepository.findByEmployeeId(userId) } returns null
        every { claimDraftRepository.save(any<ClaimDraft>()) } answers { firstArg() }
        every { fileStorageService.uploadClaimPhoto(photo, userId, 0L, any()) } returns "uploads/claim/x.jpg"
        every { storageService.getPresignedUrl("uploads/claim/x.jpg", any()) } returns "https://signed/x.jpg"

        val result = service.saveDraft(userId, request(), photo, null, null)

        assertThat(result.defectPhotoUrl).isEqualTo("https://signed/x.jpg")
        verify { fileStorageService.uploadClaimPhoto(photo, userId, 0L, any()) }
    }

    @Test
    @DisplayName("deleteDraft 는 존재하는 row 를 삭제한다")
    fun deleteDraftDeletes() {
        val draft = ClaimDraft(employeeId = userId)
        every { claimDraftRepository.findByEmployeeId(userId) } returns draft

        service.deleteDraft(userId)

        verify { claimDraftRepository.delete(draft) }
    }
}
