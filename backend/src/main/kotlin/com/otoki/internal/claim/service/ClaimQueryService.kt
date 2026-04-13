package com.otoki.internal.claim.service

import com.otoki.internal.claim.dto.response.ClaimDetailResponse
import com.otoki.internal.claim.dto.response.ClaimListItemResponse
import com.otoki.internal.claim.exception.ClaimNotFoundException
import com.otoki.internal.claim.exception.InvalidDateFormatException
import com.otoki.internal.claim.exception.InvalidDateRangeException
import com.otoki.internal.claim.repository.ClaimPhotoRepository
import com.otoki.internal.claim.repository.ClaimRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
@Transactional(readOnly = true)
class ClaimQueryService(
    private val claimRepository: ClaimRepository,
    private val claimPhotoRepository: ClaimPhotoRepository
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getClaims(userId: Long, startDateStr: String?, endDateStr: String?): List<ClaimListItemResponse> {
        val today = LocalDate.now()
        val startDate = if (startDateStr != null) parseDate(startDateStr) else today.minusDays(7)
        val endDate = if (endDateStr != null) parseDate(endDateStr) else today

        if (startDate.isAfter(endDate)) {
            throw InvalidDateRangeException()
        }

        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(LocalTime.of(23, 59, 59))

        val claims = claimRepository.findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            userId, startDateTime, endDateTime
        )

        return claims.map { ClaimListItemResponse.from(it) }
    }

    fun getClaimDetail(userId: Long, claimId: Long): ClaimDetailResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }

        if (claim.employee.id != userId) {
            throw ClaimNotFoundException(claimId)
        }

        val photos = claimPhotoRepository.findByClaimId(claimId)
        return ClaimDetailResponse.from(claim, photos)
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw InvalidDateFormatException()
        }
    }
}
