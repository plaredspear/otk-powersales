package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.dto.response.AdminClaimDetailResponse
import com.otoki.powersales.claim.dto.response.AdminClaimListItem
import com.otoki.powersales.claim.dto.response.AdminClaimListResponse
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.repository.AdminClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.PublicUrlResolver
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
    private val publicUrlResolver: PublicUrlResolver
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

        return AdminClaimListResponse(
            content = claimPage.content.map { AdminClaimListItem.Companion.from(it) },
            page = claimPage.number,
            size = claimPage.size,
            totalElements = claimPage.totalElements,
            totalPages = claimPage.totalPages
        )
    }

    fun getClaimDetail(claimId: Long): AdminClaimDetailResponse {
        val claim = adminClaimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }
        val uploadFiles: List<UploadFile> = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claim.id)
        return AdminClaimDetailResponse.Companion.from(claim, uploadFiles) { publicUrlResolver.resolve(it) }
    }
}
