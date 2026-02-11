package com.otoki.internal.repository

import com.otoki.internal.entity.ShelfLife
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 유통기한 관리 Repository
 */
@Repository
interface ShelfLifeRepository : JpaRepository<ShelfLife, Long> {

    /**
     * 사용자별 유통기한 목록 조회 (기간 필터)
     * Store, Product 정보는 비정규화 컬럼 사용으로 FetchJoin 불필요
     */
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "WHERE sl.user.id = :userId " +
        "AND sl.expiryDate BETWEEN :fromDate AND :toDate " +
        "ORDER BY sl.expiryDate ASC"
    )
    fun findByUserIdAndExpiryDateBetween(
        @Param("userId") userId: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<ShelfLife>

    /**
     * 사용자별 + 거래처별 유통기한 목록 조회 (기간 필터)
     */
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "WHERE sl.user.id = :userId " +
        "AND sl.store.id = :storeId " +
        "AND sl.expiryDate BETWEEN :fromDate AND :toDate " +
        "ORDER BY sl.expiryDate ASC"
    )
    fun findByUserIdAndStoreIdAndExpiryDateBetween(
        @Param("userId") userId: Long,
        @Param("storeId") storeId: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<ShelfLife>

    /**
     * 중복 등록 확인 (동일 사용자 + 거래처 + 제품)
     */
    fun existsByUserIdAndStoreIdAndProductId(userId: Long, storeId: Long, productId: Long): Boolean

    /**
     * 알림 발송 대상 조회 (alertDate가 오늘이고 alertSent가 false)
     */
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "JOIN FETCH sl.user " +
        "WHERE sl.alertDate = :alertDate AND sl.alertSent = false"
    )
    fun findByAlertDateAndAlertSentFalse(
        @Param("alertDate") alertDate: LocalDate
    ): List<ShelfLife>

    /**
     * 사용자 ID와 ID 목록으로 일괄 조회
     */
    fun findByIdInAndUserId(ids: List<Long>, userId: Long): List<ShelfLife>
}
