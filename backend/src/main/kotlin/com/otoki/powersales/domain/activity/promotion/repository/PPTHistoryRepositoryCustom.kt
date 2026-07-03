package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PPTHistoryRepositoryCustom {

    /**
     * 전문행사조 이력 검색.
     *
     * [teamType] 은 변경 후(newValue) 기준 필터. [teamTypeGeneral] true 면 "일반"(미지정 해제)
     * 이력만 — newValue IS NULL 조건으로 평가하며 [teamType] 은 무시된다.
     */
    fun searchHistories(
        employeeName: String?,
        employeeCode: String?,
        teamType: ProfessionalPromotionTeamType?,
        teamTypeGeneral: Boolean,
        changedAtFrom: LocalDate?,
        changedAtTo: LocalDate?,
        branchCodeFilter: List<String>?,
        pageable: Pageable
    ): Page<PPTHistorySearchResult>

    /** 사원 단위 이력 시간역순 조회 — [searchHistories] 와 동일하게 원인 마스터 거래처를 함께 적재. */
    fun findHistoriesByEmployeeId(employeeId: Long, pageable: Pageable): Page<PPTHistorySearchResult>
}

/**
 * 전문행사조 이력 + 사원 컨텍스트 + 원인 마스터(masterId FK)의 거래처 정보 projection.
 *
 * 거래처(accountCode/accountName)는 이력을 유발한 마스터(`masterId`)를 통해 조회한다.
 * `masterId` 가 null 인 이력(만료/삭제 해제·구 마이그레이션 데이터)은 거래처 두 필드가 모두 null.
 * 사원 3필드(name/code/orgName)는 기존 응답 매핑과 동일하게 employee 조인으로 함께 적재한다.
 */
data class PPTHistorySearchResult(
    val history: ProfessionalPromotionTeamHistory,
    val employeeName: String?,
    val employeeCode: String?,
    val orgName: String?,
    val accountCode: String?,
    val accountName: String?,
)
