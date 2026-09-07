package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import java.time.LocalDate

interface ClaimRepositoryCustom {

    /**
     * 본인 등록분 목록 (여사원/원가센터 미지정자).
     *
     * 레거시 SF `IF_REST_MOBILE_ClaimSearch` 와 동일하게 등록일시([Claim.createdAt]) 기준으로
     * 기간 필터링하며(종료일 당일 포함), 거래처([accountId]) 가 주어지면 해당 거래처로 추가 필터링한다(null 이면 전체).
     */
    fun findOwnClaims(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): List<Claim>

    /**
     * 같은 원가센터 전체 목록 (조장/지점장 등).
     * 등록일시([Claim.createdAt]) 기준(종료일 당일 포함) + 거래처([accountId]) 옵션 필터.
     */
    fun findCostCenterClaims(
        costCenterCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): List<Claim>
}
