package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.AdminClaimDetailResponse
import com.otoki.internal.admin.dto.response.AdminClaimListItem
import com.otoki.internal.admin.dto.response.AdminClaimListResponse
import com.otoki.internal.claim.entity.ClaimStatus
import com.otoki.internal.claim.exception.ClaimNotFoundException
import com.otoki.internal.claim.repository.AdminClaimPhotoRepository
import com.otoki.internal.claim.repository.AdminClaimRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class AdminClaimService(
    private val adminClaimRepository: AdminClaimRepository,
    private val adminClaimPhotoRepository: AdminClaimPhotoRepository
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
            content = claimPage.content.map { AdminClaimListItem.from(it) },
            page = claimPage.number,
            size = claimPage.size,
            totalElements = claimPage.totalElements,
            totalPages = claimPage.totalPages
        )
    }

    fun getClaimDetail(claimId: Long): AdminClaimDetailResponse {
        val claim = adminClaimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }
        val photos = adminClaimPhotoRepository.findByClaimId(claimId)
        return AdminClaimDetailResponse.from(claim, photos)
    }
}
