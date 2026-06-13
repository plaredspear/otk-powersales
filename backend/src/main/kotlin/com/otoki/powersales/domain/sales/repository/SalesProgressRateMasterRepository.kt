package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처목표등록마스터 Repository (SF `SalesProgressRateMaster__c`).
 *
 * SF → RDS 마이그레이션 적재 대상 entity 의 JPA Repository.
 * 마이그레이션 적재(Stage1) + admin 목록/상세 조회([SalesProgressRateMasterRepositoryCustom]) 소비.
 */
interface SalesProgressRateMasterRepository :
    JpaRepository<SalesProgressRateMaster, Long>,
    SalesProgressRateMasterRepositoryCustom {

    /** ExternalKey(`연+월+거래처코드`) 일괄 조회 — SF fetch sync 의 upsert 매칭 키. */
    fun findByExternalKeyIn(externalKeys: Collection<String>): List<SalesProgressRateMaster>

    /**
     * 거래처(account FK) + 목표연도(`"YYYY"`) 목표 행 조회 — 월매출 현황 화면 목표 source.
     *
     * `targetMonth` 는 SF 적재 포맷(zero-pad 비보장) 이라 월 일치는 호출 측에서 정수 파싱으로 판정.
     * soft-delete(`isDeleted == true`) 필터도 호출 측에서 수행 (`isDeleted` nullable).
     */
    fun findByAccountIdAndTargetYear(accountId: Long, targetYear: String): List<SalesProgressRateMaster>
}
