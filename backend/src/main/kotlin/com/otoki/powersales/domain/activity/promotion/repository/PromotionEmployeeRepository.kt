package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long>, PromotionEmployeeRepositoryCustom {

    /**
     * 행사마스터의 행사사원 일람 — soft-delete(IsDeleted) 제외 (SF 정합).
     *
     * SF 는 행사사원 조회 시 표준 SOQL 기본 동작으로 IsDeleted=true row 를 항상 제외한다.
     * is_deleted 는 nullable(SF migration row 정합)이라 `IS NULL OR = false` 로 미삭제만 통과시킨다.
     * 복제 / cascade 삭제 / 스케줄 upsert 등 promotionId 기준 전 조회가 본 메서드를 공유.
     */
    @Query(
        """
        SELECT pe FROM PromotionEmployee pe
        WHERE pe.promotionId = :promotionId
          AND (pe.isDeleted IS NULL OR pe.isDeleted = false)
        """
    )
    fun findByPromotionId(@Param("promotionId") promotionId: Long): List<PromotionEmployee>

    /** 마감(PromoCloseByTm) 행사사원 존재 여부 — soft-delete 제외 (SF 정합). */
    @Query(
        """
        SELECT CASE WHEN COUNT(pe) > 0 THEN true ELSE false END FROM PromotionEmployee pe
        WHERE pe.promotionId = :promotionId
          AND pe.promoCloseByTm = true
          AND (pe.isDeleted IS NULL OR pe.isDeleted = false)
        """
    )
    fun existsByPromotionIdAndPromoCloseByTmTrue(@Param("promotionId") promotionId: Long): Boolean

    fun deleteByPromotionId(promotionId: Long)

    /** 행사사원 중복 등록 여부 — soft-delete 제외 (SF 정합). */
    @Query(
        """
        SELECT CASE WHEN COUNT(pe) > 0 THEN true ELSE false END FROM PromotionEmployee pe
        WHERE pe.promotionId = :promotionId
          AND pe.employeeId = :employeeId
          AND (pe.isDeleted IS NULL OR pe.isDeleted = false)
        """
    )
    fun existsByPromotionIdAndEmployeeId(
        @Param("promotionId") promotionId: Long,
        @Param("employeeId") employeeId: Long,
    ): Boolean
}
