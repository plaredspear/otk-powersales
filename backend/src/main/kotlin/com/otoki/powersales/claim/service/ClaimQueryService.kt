package com.otoki.powersales.claim.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.exception.InvalidDateFormatException
import com.otoki.powersales.claim.exception.InvalidDateRangeException
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.PublicUrlResolver
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
@Transactional(readOnly = true)
class ClaimQueryService(
    private val claimRepository: ClaimRepository,
    private val employeeRepository: EmployeeRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val publicUrlResolver: PublicUrlResolver
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

        val employee = employeeRepository.findByIdOrNull(userId)
            ?: throw ClaimInvalidParameterException("사원을 찾을 수 없습니다")

        // SF 레거시 `IF_REST_MOBILE_LogisticsClaimSearch.cls:138-142` 동등 동작:
        // 여사원이면 본인 등록분, 그 외(조장/지점장 등)면 같은 원가센터 전체.
        val claims = if (employee.role == AppAuthority.WOMAN || employee.costCenterCode.isNullOrBlank()) {
            claimRepository.findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startDateTime, endDateTime
            )
        } else {
            claimRepository.findByCostCenterCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
                employee.costCenterCode!!, startDateTime, endDateTime
            )
        }

        return claims.map { ClaimListItemResponse.from(it) }
    }

    fun getClaimDetail(userId: Long, claimId: Long): ClaimDetailResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }

        val employee = employeeRepository.findByIdOrNull(userId)
            ?: throw ClaimInvalidParameterException("사원을 찾을 수 없습니다")

        // 본인 등록 OR (조장/지점장 등) 같은 원가센터 → 허용. 목록 가시 범위와 일치 (SF 레거시 동등).
        val isOwner = claim.employee?.id == userId
        val isSameCostCenter = employee.role != AppAuthority.WOMAN &&
            !employee.costCenterCode.isNullOrBlank() &&
            employee.costCenterCode == claim.costCenterCode
        if (!isOwner && !isSameCostCenter) {
            throw ClaimNotFoundException(claimId)
        }

        val photos = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimId)
        return ClaimDetailResponse.from(claim, photos) { publicUrlResolver.resolve(it) }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw InvalidDateFormatException()
        }
    }
}
