package com.otoki.internal.repository

import com.otoki.internal.entity.Inspection
import com.otoki.internal.entity.InspectionCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 현장 점검 Repository
 */
@Repository
interface InspectionRepository : JpaRepository<Inspection, Long> {

    /**
     * 사용자별 현장 점검 목록 조회 (동적 필터링)
     * 거래처ID, 분류는 선택적 (null이면 조건 미적용)
     * 점검일 기준 내림차순 정렬
     */
    @Query(
        "SELECT i FROM Inspection i " +
        "JOIN FETCH i.store " +
        "JOIN FETCH i.theme " +
        "WHERE i.user.id = :userId " +
        "AND i.inspectionDate BETWEEN :fromDate AND :toDate " +
        "AND (:storeId IS NULL OR i.store.id = :storeId) " +
        "AND (:category IS NULL OR i.category = :category) " +
        "ORDER BY i.inspectionDate DESC, i.id DESC"
    )
    fun findByUserIdWithFilters(
        @Param("userId") userId: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate,
        @Param("storeId") storeId: Long?,
        @Param("category") category: InspectionCategory?
    ): List<Inspection>

    /**
     * 현장 점검 상세 조회 (사진 포함)
     */
    @Query(
        "SELECT i FROM Inspection i " +
        "LEFT JOIN FETCH i.photos " +
        "JOIN FETCH i.store " +
        "JOIN FETCH i.theme " +
        "WHERE i.id = :inspectionId"
    )
    fun findByIdWithPhotos(
        @Param("inspectionId") inspectionId: Long
    ): Inspection?
}
