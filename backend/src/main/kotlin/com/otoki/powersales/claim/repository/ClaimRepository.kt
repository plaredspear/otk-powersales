package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ClaimRepository : JpaRepository<Claim, Long> {

    /**
     * 본인 등록분 목록 (여사원/원가센터 미지정자).
     *
     * 레거시 Heroku claim/list.jsp 와 동일하게 발생일자([Claim.date], SF ClaimDate) 기준으로
     * 기간 필터링하며, 거래처([accountId]) 가 주어지면 해당 거래처로 추가 필터링한다(null 이면 전체).
     */
    @Query(
        """
        SELECT c FROM Claim c
        WHERE c.employee.id = :employeeId
          AND c.date BETWEEN :startDate AND :endDate
          AND (:accountId IS NULL OR c.account.id = :accountId)
        ORDER BY c.date DESC, c.createdAt DESC
        """
    )
    fun findOwnClaims(
        @Param("employeeId") employeeId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("accountId") accountId: Long?
    ): List<Claim>

    /**
     * 같은 원가센터 전체 목록 (조장/지점장 등).
     * 발생일자([Claim.date]) 기준 + 거래처([accountId]) 옵션 필터.
     */
    @Query(
        """
        SELECT c FROM Claim c
        WHERE c.costCenterCode = :costCenterCode
          AND c.date BETWEEN :startDate AND :endDate
          AND (:accountId IS NULL OR c.account.id = :accountId)
        ORDER BY c.date DESC, c.createdAt DESC
        """
    )
    fun findCostCenterClaims(
        @Param("costCenterCode") costCenterCode: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("accountId") accountId: Long?
    ): List<Claim>

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

    /** SAP 인바운드 일괄 조회 (Spec #561) */
    fun findAllByNameIn(names: Collection<String>): List<Claim>
}
