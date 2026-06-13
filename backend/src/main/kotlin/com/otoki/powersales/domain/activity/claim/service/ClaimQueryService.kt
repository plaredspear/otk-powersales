package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimFormDataResponse
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.domain.activity.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateFormatException
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateRangeException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ClaimQueryService(
    private val claimRepository: ClaimRepository,
    private val employeeRepository: EmployeeRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val storageService: StorageService
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        /** 조회 기간 최대 일수 (레거시 daterangepicker maxSpan.days=7 동등). */
        private const val MAX_DATE_RANGE_DAYS = 7L
    }

    /**
     * 클레임 등록 폼 초기화 데이터(종류1/2, 구매 방법, 요청사항)를 enum 으로부터 구성한다.
     * 사용자/거래처에 무관한 정적 picklist 이므로 별도 조회 없이 즉시 반환.
     */
    fun getClaimFormData(): ClaimFormDataResponse = ClaimFormDataResponse.build()

    fun getClaims(
        userId: Long,
        startDateStr: String?,
        endDateStr: String?,
        accountId: Long?
    ): List<ClaimListItemResponse> {
        val today = LocalDate.now()
        val startDate = if (startDateStr != null) parseDate(startDateStr) else today.minusDays(7)
        val endDate = if (endDateStr != null) parseDate(endDateStr) else today

        if (startDate.isAfter(endDate)) {
            throw InvalidDateRangeException()
        }
        // 레거시 Heroku claim/list.jsp daterangepicker(maxSpan.days=7) 와 동일하게 최대 7일 범위 제한.
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_DATE_RANGE_DAYS) {
            throw ClaimInvalidParameterException("조회 기간은 최대 ${MAX_DATE_RANGE_DAYS}일까지 가능합니다")
        }

        val employee = employeeRepository.findByIdOrNull(userId)
            ?: throw ClaimInvalidParameterException("사원을 찾을 수 없습니다")

        // SF 레거시 `IF_REST_MOBILE_LogisticsClaimSearch.cls:138-142` 동등 동작:
        // 여사원이면 본인 등록분, 그 외(조장/지점장 등)면 같은 원가센터 전체.
        // 날짜 필터는 발생일자(Claim.date, SF ClaimDate) 기준 — 레거시 list.jsp 와 동일.
        val claims = if (employee.role == AppAuthority.WOMAN || employee.costCenterCode.isNullOrBlank()) {
            claimRepository.findOwnClaims(userId, startDate, endDate, accountId)
        } else {
            claimRepository.findCostCenterClaims(
                employee.costCenterCode!!, startDate, endDate, accountId
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
        // 클레임 이미지는 private/ 저장 → presigned URL 로만 조회 가능 (인증 기반 접근).
        return ClaimDetailResponse.from(claim, photos) { key ->
            key?.takeIf { it.isNotBlank() }
                ?.let { storageService.getPresignedUrl(it, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS) }
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw InvalidDateFormatException()
        }
    }
}
