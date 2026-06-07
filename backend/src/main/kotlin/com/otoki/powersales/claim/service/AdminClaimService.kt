package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.dto.response.AdminClaimDetailResponse
import com.otoki.powersales.claim.dto.response.AdminClaimListItem
import com.otoki.powersales.claim.dto.response.AdminClaimListResponse
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.repository.AdminClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileKbnTypes
import com.otoki.powersales.common.storage.UploadFileParentTypes
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class AdminClaimService(
    private val adminClaimRepository: AdminClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val storageService: StorageService
) {

    fun getClaims(
        startDate: LocalDate?,
        endDate: LocalDate?,
        status: String?,
        employeeName: String?,
        storeName: String?,
        page: Int,
        size: Int
    ): AdminClaimListResponse {
        val effectiveStartDate = startDate ?: LocalDate.now().minusDays(30)
        val effectiveEndDate = endDate ?: LocalDate.now()
        val startDateTime = effectiveStartDate.atStartOfDay()
        val endDateTime = effectiveEndDate.atTime(LocalTime.MAX)

        val claimStatus = status?.let {
            try { ClaimStatus.valueOf(it) } catch (_: IllegalArgumentException) { null }
        }

        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val claimPage = adminClaimRepository.findClaims(
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            status = claimStatus,
            employeeName = employeeName,
            storeName = storeName,
            pageable = pageable
        )

        val claims = claimPage.content
        val claimIds = claims.map { it.id }
        val filesByClaimId: Map<Long, List<UploadFile>> =
            if (claimIds.isEmpty()) {
                emptyMap()
            } else {
                uploadFileRepository
                    .findByParentTypeAndParentIdInAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimIds)
                    .filter { it.parentId != null }
                    .groupBy { it.parentId!! }
            }

        return AdminClaimListResponse(
            content = claims.map { claim ->
                val imageUrl = resolveRepresentativeImageUrl(filesByClaimId[claim.id].orEmpty())
                AdminClaimListItem.from(claim, imageUrl)
            },
            page = claimPage.number,
            size = claimPage.size,
            totalElements = claimPage.totalElements,
            totalPages = claimPage.totalPages
        )
    }

    /**
     * 클레임 첨부 사진 중 목록 카드 배경용 대표 이미지 URL 을 선정한다.
     * - uniqueKey 가 없는 파일은 후보에서 제외 (URL 변환 불가).
     * - 불량(CLAIM_DEFECT) 사진 우선, 동급이면 createdAt 최소(첫 사진).
     * - 후보가 없으면 null.
     */
    private fun resolveRepresentativeImageUrl(files: List<UploadFile>): String? {
        val uniqueKey = files
            .filter { !it.uniqueKey.isNullOrBlank() }
            .minWithOrNull(
                compareByDescending<UploadFile> { it.uploadKbn == UploadFileKbnTypes.CLAIM_DEFECT }
                    .thenBy { it.createdAt }
            )
            ?.uniqueKey ?: return null
        // 클레임 이미지는 private/ 저장 → presigned URL 로만 조회 가능 (인증 기반 접근).
        return storageService.getPresignedUrl(uniqueKey, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS)
    }

    fun getClaimDetail(claimId: Long): AdminClaimDetailResponse {
        val claim = adminClaimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }
        val uploadFiles: List<UploadFile> = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claim.id)
        return AdminClaimDetailResponse.Companion.from(claim, uploadFiles) { key ->
            key?.takeIf { it.isNotBlank() }
                ?.let { storageService.getPresignedUrl(it, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS) }
        }
    }
}
