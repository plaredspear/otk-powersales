package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

interface AdminClaimRepositoryCustom {

    fun findClaims(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        status: ClaimStatus?,
        employeeName: String?,
        storeName: String?,
        pageable: Pageable
    ): Page<Claim>

    /**
     * 기간별 클레임 보고서 조회 (Spec #843 — SF Report `X3_ONLY_veg`/`X4_3xv` 이식).
     * `claim` ⋈ employee ⋈ account ⋈ product. 페이지네이션 없이 전량 추출 (전사).
     * 필터: claim.date(ClaimDate) ∈ [startDate, endDate], status='전송완료'(SENT),
     *       claimType1 지정 시 해당 대분류만 (PACKAGING=A) / null 이면 전체(모든 클레임 = A,B,C).
     * 정렬: date 내림차순.
     */
    fun findPeriodReport(
        startDate: LocalDate,
        endDate: LocalDate,
        claimType1: ClaimType1?,
    ): List<Claim>
}
