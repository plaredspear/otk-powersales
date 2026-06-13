package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.ClaimDraftRequest
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimDraftResponse
import com.otoki.powersales.domain.activity.claim.entity.ClaimDraft
import com.otoki.powersales.domain.activity.claim.repository.ClaimDraftRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileKbnTypes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

/**
 * 클레임 임시저장(draft) 서비스.
 *
 * ## 레거시 동작 (otg_PowerSales FieldTalkController.tempClaimProc)
 * - 임시저장은 검증 없이 `tmp_claim` 에 upsert. 사원 1명당 1건.
 * - 클레임 등록 화면 진입 시 임시저장이 있으면 "이어서 작성?" 후 prefill.
 * - 정식 등록 성공 시 임시저장 row 삭제.
 *
 * ## 신규 차이 (deviation)
 * - 임시저장은 신규 `claim_draft` 테이블([ClaimDraft])로 대체 (tmp_claim 대응, [DailySalesDraft] 패턴 정합).
 * - 사진은 base64 DB 보관 대신 S3 private 업로드 후 key 만 보관([FileStorageService.uploadClaimPhoto]).
 *   조회 시 presigned URL 로 변환해 내려준다.
 *
 * ## S3 객체 수명주기
 * 교체된 이전 S3 객체는 트랜잭션 롤백 시 데이터 손실을 막기 위해 즉시 삭제하지 않는다.
 * 고아 객체는 S3 lifecycle 정책으로 정리한다([MobileDailySalesService] 정합).
 */
@Service
@Transactional(readOnly = true)
class ClaimDraftService(
    private val claimDraftRepository: ClaimDraftRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService
) {

    /** 임시저장 조회. 없으면 null. */
    fun getDraft(userId: Long): ClaimDraftResponse? {
        val draft = claimDraftRepository.findByEmployeeId(userId) ?: return null
        return ClaimDraftResponse.from(draft, ::resolveUrl)
    }

    /** 임시저장 upsert (검증 없음). 전달된 사진만 갱신한다. */
    @Transactional
    fun saveDraft(
        userId: Long,
        request: ClaimDraftRequest,
        defectPhoto: MultipartFile?,
        labelPhoto: MultipartFile?,
        receiptPhoto: MultipartFile?
    ): ClaimDraftResponse {
        val draft = claimDraftRepository.findByEmployeeId(userId)
            ?: ClaimDraft(employeeId = userId)

        draft.apply(
            accountId = request.accountId,
            accountName = request.accountName,
            productCode = request.productCode,
            productName = request.productName,
            dateType = request.dateType,
            claimDate = parseDateOrNull(request.date),
            claimType1 = request.claimType1,
            claimType2 = request.claimType2,
            defectDescription = request.defectDescription,
            defectQuantity = request.defectQuantity,
            purchaseAmount = request.purchaseAmount,
            purchaseMethodCode = request.purchaseMethodCode,
            requestTypeCode = request.requestTypeCode
        )

        uploadPhotoOrNull(defectPhoto, userId, UploadFileKbnTypes.CLAIM_DEFECT)
            ?.let { draft.defectPhotoKey = it }
        uploadPhotoOrNull(labelPhoto, userId, UploadFileKbnTypes.CLAIM_PART)
            ?.let { draft.labelPhotoKey = it }
        uploadPhotoOrNull(receiptPhoto, userId, UploadFileKbnTypes.CLAIM_RECEIPT)
            ?.let { draft.receiptPhotoKey = it }

        val saved = claimDraftRepository.save(draft)
        return ClaimDraftResponse.from(saved, ::resolveUrl)
    }

    /** 임시저장 폐기. */
    @Transactional
    fun deleteDraft(userId: Long) {
        val draft = claimDraftRepository.findByEmployeeId(userId) ?: return
        claimDraftRepository.delete(draft)
    }

    // ───── private helpers ─────

    private fun uploadPhotoOrNull(photo: MultipartFile?, userId: Long, uploadKbn: String): String? {
        if (photo == null || photo.isEmpty) return null
        // 임시저장은 claim row 가 없으므로 claimId 자리에 0 을 넘긴다(uploadClaimPhoto 는 claimId 를 key 에 사용하지 않음).
        return fileStorageService.uploadClaimPhoto(photo, userId, 0L, uploadKbn)
    }

    private fun resolveUrl(key: String?): String? =
        key?.takeIf { it.isNotBlank() }
            ?.let { storageService.getPresignedUrl(it, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS) }

    /** 임시저장은 날짜가 부분 입력일 수 있으므로 파싱 실패 시 null 로 저장한다. */
    private fun parseDateOrNull(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDate.parse(raw)
        } catch (e: Exception) {
            null
        }
    }
}
