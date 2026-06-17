package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionDraftRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionDraftResponse
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionDraft
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionDraftRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 제안 임시저장(draft) 서비스.
 *
 * ## 레거시 동작 (otg_PowerSales FieldTalkController.tempSuggestProc)
 * - 임시저장은 검증 없이 `tmp_suggest` 에 upsert. 사원 1명당 1건.
 * - 제안 등록 화면 진입 시 임시저장이 있으면 "이어서 작성?" 후 prefill.
 * - 정식 등록 성공 시 임시저장 row 삭제([SuggestionService.create] 에서 처리).
 *
 * ## 신규 차이 (deviation)
 * - 임시저장은 신규 `suggestion_draft` 테이블([SuggestionDraft])로 대체
 *   ([com.otoki.powersales.domain.activity.claim.service.ClaimDraftService] 패턴 정합).
 * - 사진은 base64 DB 보관 대신 S3 private 업로드 후 key 만 보관, 조회 시 presigned URL 로 변환.
 *
 * ## 사진 갱신 규칙
 * 사진 part(`photos`)가 전달되면 전달된 set 으로 **전체 교체**(1번째→photoKey1, 2번째→photoKey2),
 * 전달되지 않으면(null/빈 목록) 기존 사진을 유지한다. 레거시 tmp_suggest 와 동일하게 최대 2장.
 *
 * ## S3 객체 수명주기
 * 교체된 이전 S3 객체는 트랜잭션 롤백 시 데이터 손실을 막기 위해 즉시 삭제하지 않는다.
 * 고아 객체는 S3 lifecycle 정책으로 정리한다(ClaimDraftService 정합).
 */
@Service
@Transactional(readOnly = true)
class SuggestionDraftService(
    private val suggestionDraftRepository: SuggestionDraftRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService
) {

    /** 임시저장 조회. 없으면 null. */
    fun getDraft(userId: Long): SuggestionDraftResponse? {
        val draft = suggestionDraftRepository.findByEmployeeId(userId) ?: return null
        return SuggestionDraftResponse.from(draft, ::resolveUrl)
    }

    /** 임시저장 upsert (검증 없음). */
    @Transactional
    fun saveDraft(
        userId: Long,
        request: SuggestionDraftRequest,
        photos: List<MultipartFile>?
    ): SuggestionDraftResponse {
        val draft = suggestionDraftRepository.findByEmployeeId(userId)
            ?: SuggestionDraft(employeeId = userId)

        draft.apply(
            category = request.category,
            title = request.title,
            content = request.content,
            productCode = request.productCode,
            productName = request.productName,
            accountId = request.accountId,
            accountName = request.accountName,
            sapAccountCode = request.sapAccountCode,
            claimType = request.claimType,
            claimDate = request.claimDate,
            carNumber = request.carNumber,
            logisticsResponsibility = request.logisticsResponsibility,
            duplicateProposalNum = request.duplicateProposalNum,
            actionStatus = request.actionStatus
        )

        // 사진 part 가 전달되면 전체 교체(최대 2장), 없으면 기존 유지.
        val nonEmptyPhotos = photos?.filterNot { it.isEmpty }.orEmpty()
        if (nonEmptyPhotos.isNotEmpty()) {
            draft.photoKey1 = uploadPhoto(nonEmptyPhotos[0])
            draft.photoKey2 = nonEmptyPhotos.getOrNull(1)?.let { uploadPhoto(it) }
        }

        val saved = suggestionDraftRepository.save(draft)
        return SuggestionDraftResponse.from(saved, ::resolveUrl)
    }

    /** 임시저장 폐기. */
    @Transactional
    fun deleteDraft(userId: Long) {
        val draft = suggestionDraftRepository.findByEmployeeId(userId) ?: return
        suggestionDraftRepository.delete(draft)
    }

    // ───── private helpers ─────

    // 임시저장은 suggestion row 가 없으므로 suggestionId 자리에 0 을 넘긴다
    // (uploadSuggestionPhoto 는 suggestionId 를 key 에 사용하지 않음).
    private fun uploadPhoto(photo: MultipartFile): String =
        fileStorageService.uploadSuggestionPhoto(photo, 0L)

    private fun resolveUrl(key: String?): String? =
        key?.takeIf { it.isNotBlank() }
            ?.let { storageService.getPresignedUrl(it, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS) }
}
