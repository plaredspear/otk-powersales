package com.otoki.powersales.inspection.service

import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.inspection.dto.request.SiteActivityDraftRequest
import com.otoki.powersales.inspection.dto.response.SiteActivityDraftResponse
import com.otoki.powersales.inspection.entity.SiteActivityDraft
import com.otoki.powersales.inspection.repository.SiteActivityDraftRepository
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 현장점검 임시저장(draft) 서비스.
 *
 * ## 레거시 동작 (otg_PowerSales FieldTalkController.tempFieldChkProc)
 * - 임시저장은 검증 없이 `tmp_onsite` 에 upsert. 사원 1명당 1건.
 * - 현장점검 등록 화면 진입 시 임시저장이 있으면 "이어서 작성?" 후 prefill.
 * - 정식 등록 성공 시 임시저장 row 삭제([SiteActivityService.register] 에서 수행).
 *
 * ## 신규 차이 (deviation)
 * - 임시저장은 신규 `site_activity_draft` 테이블([SiteActivityDraft])로 대체 (tmp_onsite 대응, claim_draft 패턴 정합).
 * - 사진은 등록과 동일 도메인("site-activity") private 업로드 후 key 만 보관, 조회 시 presigned URL 로 변환.
 * - 사진은 전달된 목록으로 전체 교체한다(복원 시 임시파일로 재첨부되어 다음 저장에 재전송되므로 안전).
 */
@Service
@Transactional(readOnly = true)
class SiteActivityDraftService(
    private val siteActivityDraftRepository: SiteActivityDraftRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 2
    }

    /** 임시저장 조회. 없으면 null. */
    fun getDraft(userId: Long): SiteActivityDraftResponse? {
        val draft = siteActivityDraftRepository.findByEmployeeId(userId) ?: return null
        return SiteActivityDraftResponse.from(draft, ::resolveUrl)
    }

    /** 임시저장 upsert (검증 없음). 사진은 전달된 목록(최대 2장)으로 전체 교체한다. */
    @Transactional
    fun saveDraft(
        userId: Long,
        request: SiteActivityDraftRequest,
        photos: List<MultipartFile>?
    ): SiteActivityDraftResponse {
        val draft = siteActivityDraftRepository.findByEmployeeId(userId)
            ?: SiteActivityDraft(employeeId = userId)

        draft.apply(
            themeId = request.themeId,
            category = request.category,
            accountId = request.accountId,
            accountName = request.accountName,
            inspectionDate = parseDateOrNull(request.inspectionDate),
            fieldTypeCode = request.fieldTypeCode,
            description = request.description,
            productCode = request.productCode,
            productName = request.productName,
            competitorName = request.competitorName,
            competitorActivity = request.competitorActivity,
            competitorTasting = request.competitorTasting,
            competitorProductName = request.competitorProductName,
            competitorProductPrice = request.competitorProductPrice,
            competitorSalesQuantity = request.competitorSalesQuantity
        )

        val keys = (photos ?: emptyList())
            .filterNot { it.isEmpty }
            .take(MAX_PHOTO_COUNT)
            // 임시저장은 site_activity row 가 없으므로 id 자리에 0 을 넘긴다(uploadSiteActivityPhoto 는 id 를 key 에 쓰지 않음).
            .map { fileStorageService.uploadSiteActivityPhoto(it, 0L) }
        draft.photoKey1 = keys.getOrNull(0)
        draft.photoKey2 = keys.getOrNull(1)

        val saved = siteActivityDraftRepository.save(draft)
        return SiteActivityDraftResponse.from(saved, ::resolveUrl)
    }

    /** 임시저장 폐기. */
    @Transactional
    fun deleteDraft(userId: Long) {
        val draft = siteActivityDraftRepository.findByEmployeeId(userId) ?: return
        siteActivityDraftRepository.delete(draft)
    }

    // ───── private helpers ─────

    private fun resolveUrl(key: String?): String? =
        key?.takeIf { it.isNotBlank() }
            ?.let { storageService.getPresignedUrl(it, StorageConstants.SITE_ACTIVITY_PRESIGN_TTL_SECONDS) }

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
